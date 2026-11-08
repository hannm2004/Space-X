package vn.spacex.image;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.lesson.LessonNotFoundException;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;

import java.util.List;

@Service
public class ImageService {

    public static final int MAX_BYTES = 1_048_576;
    public static final String TOO_LARGE_DETAIL = "Ảnh vượt quá 1 MB";

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final LessonImageRepository imageRepository;
    private final LessonRepository lessonRepository;

    public ImageService(LessonImageRepository imageRepository, LessonRepository lessonRepository) {
        this.imageRepository = imageRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional
    public ImageUploadResponse upload(Long lessonId, String originalFileName, byte[] bytes) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException();
        }
        if (bytes.length > MAX_BYTES) {
            throw new ImageTooLargeException();
        }
        ImageType type = ImageInspector.detect(bytes).orElseThrow(UnsupportedImageTypeException::new);
        LessonImage image = LessonImage.create(lessonId, cleanFileName(originalFileName, type),
                type.contentType(), bytes);
        try {
            Long id = imageRepository.saveAndFlush(image).getId();
            return new ImageUploadResponse(id, urlOf(id));
        } catch (DataIntegrityViolationException e) {
            // Bài vừa bị xóa giữa lúc kiểm tra và lúc ghi: khóa ngoại lesson_id báo lỗi.
            throw new LessonNotFoundException();
        }
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> list(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException();
        }
        return imageRepository.findByLessonIdOrderByIdAsc(lessonId).stream()
                .map(info -> new ImageResponse(info.getId(), info.getFileName(), info.getContentType(),
                        info.getSizeBytes(), urlOf(info.getId())))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!imageRepository.existsById(id)) {
            throw new ImageNotFoundException();
        }
        imageRepository.deleteById(id);
    }

    // Học sinh chỉ xem được ảnh của bài đã đăng; Admin xem được cả ảnh của bài nháp. Ảnh không tồn tại và ảnh của bài
    // nháp trả cùng một lỗi nên người ngoài không dò được id nào thuộc bài nháp. Dữ liệu ảnh chỉ được tải sau khi qua kiểm quyền.
    @Transactional(readOnly = true)
    public StoredImage load(Long id, boolean canSeeDrafts) {
        LessonImageInfo info = imageRepository.findById(id, LessonImageInfo.class)
                .orElseThrow(ImageNotFoundException::new);
        if (!canSeeDrafts && !isPublished(info.getLessonId())) {
            throw new ImageNotFoundException();
        }
        LessonImage image = imageRepository.findById(id).orElseThrow(ImageNotFoundException::new);
        return new StoredImage(image.getContentType(), image.getData());
    }

    private boolean isPublished(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .map(lesson -> lesson.getStatus() == LessonStatus.PUBLISHED)
                .orElse(false);
    }

    static String urlOf(Long id) {
        return "/api/images/" + id;
    }

    // Tên chỉ để hiển thị. Bỏ phần đường dẫn (cả / lẫn \) và ký tự điều khiển; thiếu hoặc rỗng thì đặt tên mặc định
    // theo loại ảnh thật. Cắt cho vừa cột VARCHAR(255) mà không tách đôi cặp thay thế (surrogate).
    static String cleanFileName(String original, ImageType type) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cc}", "").trim();
        if (name.isEmpty()) {
            return "image." + type.extension();
        }
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            int end = Character.isHighSurrogate(name.charAt(MAX_FILE_NAME_LENGTH - 1))
                    ? MAX_FILE_NAME_LENGTH - 1 : MAX_FILE_NAME_LENGTH;
            name = name.substring(0, end);
        }
        return name;
    }
}
