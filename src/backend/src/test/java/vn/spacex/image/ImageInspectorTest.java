package vn.spacex.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ImageInspectorTest {

    static byte[] png(int width, int height) {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png");
    }

    static byte[] jpeg(int width, int height) {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg");
    }

    // Ảnh nhị phân rất nén: 5000x5000 chỉ vài KB nhưng giải nén ra hàng chục triệu điểm ảnh.
    static byte[] pngNenManh(int width, int height) {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY), "png");
    }

    private static byte[] encode(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // Dựng một tệp WebP đúng cấu trúc RIFF: "RIFF" + kích thước + "WEBP" + khối đầu tiên.
    static byte[] webp(String fourcc, int payloadLength) {
        ByteBuffer buf = ByteBuffer.allocate(20 + payloadLength).order(ByteOrder.LITTLE_ENDIAN);
        buf.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(buf.capacity() - 8);
        buf.put("WEBP".getBytes(StandardCharsets.US_ASCII));
        buf.put(fourcc.getBytes(StandardCharsets.US_ASCII));
        buf.putInt(payloadLength);
        return buf.array();
    }

    private static Optional<ImageType> detect(byte[] bytes) {
        return ImageInspector.detect(bytes);
    }

    @Test
    void nhanRaPngThatVaTraVeContentTypeDungTheoNoiDung() {
        assertThat(detect(png(10, 10))).contains(ImageType.PNG);
        assertThat(ImageType.PNG.contentType()).isEqualTo("image/png");
    }

    @Test
    void nhanRaJpegThat() {
        assertThat(detect(jpeg(10, 10))).contains(ImageType.JPEG);
        assertThat(ImageType.JPEG.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void nhanRaWebpDungCauTrucRiffChoCaBaKieuKhoi() {
        assertThat(detect(webp("VP8 ", 10))).contains(ImageType.WEBP);
        assertThat(detect(webp("VP8L", 10))).contains(ImageType.WEBP);
        assertThat(detect(webp("VP8X", 10))).contains(ImageType.WEBP);
        assertThat(ImageType.WEBP.contentType()).isEqualTo("image/webp");
    }

    @Test
    void anhDuocPhepDenKichThuocToiDa4096MoiCanhNhungKhongQua() {
        assertThat(detect(png(4096, 100))).contains(ImageType.PNG);
        assertThat(detect(png(100, 4096))).contains(ImageType.PNG);
        assertThat(detect(pngNenManh(4097, 100))).isEmpty();
        assertThat(detect(pngNenManh(100, 4097))).isEmpty();
    }

    @Test
    void anhNenManhRatLonBiTuChoiMaKhongPhaiGiaiNenRaBoNho() {
        byte[] bom = pngNenManh(6000, 6000);

        assertThat(bom.length).isLessThan(1_048_576);
        assertThat(detect(bom)).isEmpty();
    }

    @Test
    void khongPhaiAnhThiBiTuChoiDuNoGiVoDuoiHayContentTypeNao() {
        assertThat(detect(new byte[0])).isEmpty();
        assertThat(detect("chi la van ban".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(detect("<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(detect("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(detect("GIF89a....".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(detect(new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03})).isEmpty();
    }

    @Test
    void chuKyPngNhungPhanConLaRacBiTuChoi() {
        byte[] gia = Arrays.copyOf(png(10, 10), 8);
        byte[] giaCoRac = new byte[200];
        System.arraycopy(gia, 0, giaCoRac, 0, 8);

        assertThat(detect(gia)).isEmpty();
        assertThat(detect(giaCoRac)).isEmpty();
    }

    @Test
    void chuKyJpegNhungPhanConLaRacBiTuChoi() {
        byte[] giaCoRac = new byte[200];
        giaCoRac[0] = (byte) 0xFF;
        giaCoRac[1] = (byte) 0xD8;
        giaCoRac[2] = (byte) 0xFF;

        assertThat(detect(giaCoRac)).isEmpty();
    }

    @Test
    void pngBiCatCutGiuaChungKhongDocDuocKichThuocBiTuChoi() {
        byte[] full = png(50, 50);

        assertThat(detect(Arrays.copyOf(full, 20))).isEmpty();
    }

    @Test
    void riffNhungKhongPhaiWebpBiTuChoi() {
        byte[] wave = webp("VP8L", 10);
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, wave, 8, 4);

        assertThat(detect(wave)).isEmpty();
    }

    @Test
    void webpSaiKichThuocRiffHoacKhoiDauLaBiTuChoi() {
        byte[] saiKichThuoc = webp("VP8L", 10);
        saiKichThuoc[4] = 0x7F;
        byte[] khoiLa = webp("JUNK", 10);
        byte[] khoiVuotQuaTep = webp("VP8L", 10);
        khoiVuotQuaTep[16] = (byte) 0xFF;

        assertThat(detect(saiKichThuoc)).isEmpty();
        assertThat(detect(khoiLa)).isEmpty();
        assertThat(detect(khoiVuotQuaTep)).isEmpty();
        assertThat(detect(Arrays.copyOf(webp("VP8L", 10), 15))).isEmpty();
    }

    @Test
    void nullBiTuChoiThayVaNemLoi() {
        assertThat(detect(null)).isEmpty();
    }
}
