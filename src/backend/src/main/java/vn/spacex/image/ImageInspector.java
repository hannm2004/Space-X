package vn.spacex.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

// Nhận diện loại ảnh từ NỘI DUNG, không tin đuôi tệp hay Content-Type do trình duyệt khai báo.
// Không dùng thư viện ngoài: chữ ký byte + bộ đọc ảnh có sẵn của JDK (chỉ đọc phần đầu tệp để lấy kích thước).
final class ImageInspector {

    // Chặn "bom giải nén": PNG trắng 6000x6000 chỉ vài KB nhưng trình duyệt phải dựng hàng chục triệu điểm ảnh.
    private static final int MAX_SIDE = 4096;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final Set<String> WEBP_FIRST_CHUNKS = Set.of("VP8 ", "VP8L", "VP8X");

    private ImageInspector() {
    }

    static Optional<ImageType> detect(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return Optional.empty();
        }
        if (startsWith(bytes, PNG_SIGNATURE)) {
            return headerReadable(bytes) ? Optional.of(ImageType.PNG) : Optional.empty();
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return headerReadable(bytes) ? Optional.of(ImageType.JPEG) : Optional.empty();
        }
        if (isWebp(bytes)) {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    // JDK không đọc được WebP nên kiểm cấu trúc RIFF: "RIFF" + kích thước + "WEBP" + khối đầu tiên hợp lệ.
    private static boolean isWebp(byte[] bytes) {
        if (bytes.length < 20 || !ascii(bytes, 0, 4).equals("RIFF") || !ascii(bytes, 8, 4).equals("WEBP")) {
            return false;
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        long riffSize = Integer.toUnsignedLong(buf.getInt(4));
        long chunkSize = Integer.toUnsignedLong(buf.getInt(16));
        return riffSize == bytes.length - 8L
                && WEBP_FIRST_CHUNKS.contains(ascii(bytes, 12, 4))
                && chunkSize <= bytes.length - 20L;
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    // Đọc kích thước từ phần đầu tệp (không giải mã cả ảnh). Tệp có chữ ký đúng nhưng phần còn lại là rác sẽ lỗi ở đây.
    private static boolean headerReadable(byte[] bytes) {
        // MemoryCacheImageInputStream giữ dữ liệu trong bộ nhớ; ImageIO.createImageInputStream có thể ghi tệp tạm.
        try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return width >= 1 && height >= 1 && width <= MAX_SIDE && height <= MAX_SIDE;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            // Tệp hỏng/cụt/giả: bộ đọc ném IIOException hoặc lỗi chỉ mục. Với người gọi đó chỉ là "không phải ảnh hợp lệ".
            return false;
        }
    }
}
