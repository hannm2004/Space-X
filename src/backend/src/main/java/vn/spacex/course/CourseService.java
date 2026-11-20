package vn.spacex.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.image.CourseCoverService;
import vn.spacex.image.CoverImageLookup;
import vn.spacex.lesson.Lesson;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;
import vn.spacex.lesson.LessonSummaryResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CoverImageLookup coverImageLookup;
    private final CourseCoverService courseCoverService;

    public CourseService(CourseRepository courseRepository, LessonRepository lessonRepository,
                         CoverImageLookup coverImageLookup, CourseCoverService courseCoverService) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.coverImageLookup = coverImageLookup;
        this.courseCoverService = courseCoverService;
    }

    // Mỗi khóa kèm đường dẫn ảnh bìa (null nếu chưa có), lấy bằng một truy vấn cho cả danh sách.
    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses() {
        List<Course> courses = courseRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, String> slugsById = new LinkedHashMap<>();
        courses.forEach(course -> slugsById.put(course.getId(), course.getSlug()));
        Map<Long, String> covers = courseCoverService.urlsByCourseId(slugsById);
        return courses.stream()
                .map(course -> CourseResponse.from(course, covers.get(course.getId())))
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
