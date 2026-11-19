package vn.spacex.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.image.CoverImageLookup;
import vn.spacex.lesson.Lesson;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;
import vn.spacex.lesson.LessonSummaryResponse;

import java.util.List;
import java.util.Map;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CoverImageLookup coverImageLookup;

    public CourseService(CourseRepository courseRepository, LessonRepository lessonRepository,
                         CoverImageLookup coverImageLookup) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.coverImageLookup = coverImageLookup;
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
        List<Lesson> lessons = lessonRepository
                .findByCourseIdAndStatusOrderBySortOrderAscIdAsc(course.getId(), LessonStatus.PUBLISHED);
        Map<Long, String> covers = coverImageLookup.urlsByLessonId(lessons.stream().map(Lesson::getId).toList());
        return lessons.stream()
                .map(lesson -> LessonSummaryResponse.from(lesson, covers.get(lesson.getId())))
                .toList();
    }
}
