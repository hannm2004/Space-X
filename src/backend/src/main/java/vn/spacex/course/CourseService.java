package vn.spacex.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;
import vn.spacex.lesson.LessonSummaryResponse;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    public CourseService(CourseRepository courseRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses() {
        return courseRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(CourseResponse::from)
                .toList();
    }

    // Khóa không tồn tại thì 404; khóa có nhưng chưa có bài đã đăng thì trả danh sách rỗng, không phải lỗi.
    // Bài DRAFT bị loại ngay ở truy vấn, nên bài nháp không bao giờ tới được người gọi công khai.
    @Transactional(readOnly = true)
    public List<LessonSummaryResponse> listPublishedLessons(String courseSlug) {
        Course course = courseRepository.findBySlug(courseSlug).orElseThrow(CourseNotFoundException::new);
        return lessonRepository
                .findByCourseIdAndStatusOrderBySortOrderAscIdAsc(course.getId(), LessonStatus.PUBLISHED).stream()
                .map(LessonSummaryResponse::from)
                .toList();
    }
}
