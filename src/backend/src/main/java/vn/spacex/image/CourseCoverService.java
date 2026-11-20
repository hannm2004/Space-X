package vn.spacex.image;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.course.Course;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.course.CourseRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Ảnh bìa khóa học (D35). Dùng lại cách kiểm ảnh của ảnh bài học (cỡ tối đa 1 MB, loại nhận diện từ nội dung, chỉ
// PNG/JPEG/WebP) nhưng khác quyền xem: ảnh bìa khóa công khai như danh sách khóa, vì trang chủ hiện cho khách.
@Service
public class CourseCoverService {

    private final CourseCoverRepository coverRepository;
    private final CourseRepository courseRepository;
    private final Clock clock;

    public CourseCoverService(CourseCoverRepository coverRepository, CourseRepository courseRepository, Clock clock) {
        this.coverRepository = coverRepository;
        this.courseRepository = courseRepository;
        this.clock = clock;
    }

    // Đặt ảnh bìa mới thì ghi đè ảnh cũ (khóa chính là mã khóa).
    @Transactional
    public CourseCoverResponse setCover(Long courseId, byte[] bytes) {
        Course course = courseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
        if (bytes.length > ImageService.MAX_BYTES) {
            throw new ImageTooLargeException();
        }
        ImageType type = ImageInspector.detect(bytes).orElseThrow(UnsupportedImageTypeException::new);
        // Cắt về mili giây: đường dẫn trả về lúc đặt và đường dẫn trong danh sách (đọc lại từ MySQL) phải trùng nhau.
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        try {
            coverRepository.saveAndFlush(CourseCover.create(courseId, type.contentType(), bytes, now));
        } catch (DataIntegrityViolationException e) {
            // Khóa vừa bị xóa giữa lúc kiểm tra và lúc ghi: khóa ngoại course_id báo lỗi.
            throw new CourseNotFoundException();
        }
        return new CourseCoverResponse(urlOf(course.getSlug(), now));
    }

    // Bỏ ảnh bìa khi khóa chưa có ảnh vẫn thành công (kết quả cuối cùng như nhau).
    @Transactional
    public void clearCover(Long courseId) {
        if (courseRepository.findById(courseId).isEmpty()) {
            throw new CourseNotFoundException();
        }
        if (coverRepository.existsById(courseId)) {
            coverRepository.deleteById(courseId);
        }
    }

    // Công khai: khóa không tồn tại và khóa chưa có ảnh đều 404 (hai lỗi khác nhau, cùng trạng thái).
    @Transactional(readOnly = true)
    public StoredImage load(String slug) {
        Course course = courseRepository.findBySlug(slug).orElseThrow(CourseNotFoundException::new);
        CourseCover cover = coverRepository.findById(course.getId()).orElseThrow(ImageNotFoundException::new);
        return new StoredImage(cover.getContentType(), cover.getData());
    }

    // Gắn ảnh bìa vào danh sách khóa: một truy vấn cho cả danh sách. Khóa chưa có ảnh không có trong kết quả.
    @Transactional(readOnly = true)
    public Map<Long, String> urlsByCourseId(Map<Long, String> slugsById) {
        Map<Long, String> urls = new HashMap<>();
        if (slugsById.isEmpty()) {
            return urls;
        }
        for (CourseCoverVersion version : coverRepository.findVersions(List.copyOf(slugsById.keySet()))) {
            urls.put(version.getCourseId(), urlOf(slugsById.get(version.getCourseId()), version.getUpdatedAt()));
        }
        return urls;
    }

    // Đường dẫn ảnh bìa của một khóa, hoặc null nếu khóa chưa có ảnh.
    @Transactional(readOnly = true)
    public String urlOfCourse(Long courseId, String slug) {
        return urlsByCourseId(Map.of(courseId, slug)).get(courseId);
    }

    // ?v= là mốc thời gian đặt ảnh: đổi ảnh thì đường dẫn đổi, trình duyệt không giữ ảnh bìa cũ.
    static String urlOf(String slug, LocalDateTime updatedAt) {
        return "/api/courses/" + slug + "/cover?v=" + updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
