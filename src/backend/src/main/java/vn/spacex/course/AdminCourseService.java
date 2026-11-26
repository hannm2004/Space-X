package vn.spacex.course;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.image.CourseCoverService;
import vn.spacex.lesson.LessonRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCourseService {

    // Do migration V6 tạo. Khóa tạo mới vào nhóm này cho tới khi form Admin cho chọn nhóm.
    private static final String DEFAULT_CATEGORY_SLUG = "khoa-cho-thieu-nien";

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseCoverService courseCoverService;
    private final CategoryRepository categoryRepository;

    public AdminCourseService(CourseRepository courseRepository, LessonRepository lessonRepository,
                              CourseCoverService courseCoverService, CategoryRepository categoryRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.courseCoverService = courseCoverService;
        this.categoryRepository = categoryRepository;
    }

    // Khác GET /api/courses (công khai, không có sortOrder): form sửa khóa cần giá trị sortOrder hiện tại.
    @Transactional(readOnly = true)
    public List<AdminCourseResponse> findAll() {
        List<Course> courses = courseRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, String> slugsById = new LinkedHashMap<>();
        courses.forEach(course -> slugsById.put(course.getId(), course.getSlug()));
        Map<Long, String> covers = courseCoverService.urlsByCourseId(slugsById);
        return courses.stream()
                .map(course -> AdminCourseResponse.from(course, covers.get(course.getId())))
                .toList();
    }

    @Transactional
    public AdminCourseResponse create(AdminCourseRequest request) {
        if (courseRepository.existsBySlug(request.slug())) {
            throw new CourseSlugAlreadyExistsException();
        }
        Course course = Course.create(request.slug(), request.title(), request.description(), request.sortOrder());
        Category category = categoryRepository.findBySlug(DEFAULT_CATEGORY_SLUG)
                .orElseThrow(() -> new IllegalStateException("Thiếu nhóm khóa mặc định"));
        course.placeIn(category.getId(), CourseCodes.fromSlug(request.slug(), courseRepository::existsByCode));
        // Khóa mới chưa có ảnh bìa.
        return AdminCourseResponse.from(persist(course), null);
    }

    @Transactional
    public AdminCourseResponse update(Long id, AdminCourseRequest request) {
        Course course = courseRepository.findById(id).orElseThrow(CourseNotFoundException::new);
        if (courseRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new CourseSlugAlreadyExistsException();
        }
        course.update(request.slug(), request.title(), request.description(), request.sortOrder());
        Course saved = persist(course);
        // Sửa khóa không đụng tới ảnh bìa; trả đường dẫn hiện có để trang Admin không mất ảnh vừa hiện.
        return AdminCourseResponse.from(saved, courseCoverService.urlOfCourse(id, saved.getSlug()));
    }

    @Transactional
    public void delete(Long id) {
        Course course = courseRepository.findById(id).orElseThrow(CourseNotFoundException::new);
        if (lessonRepository.existsByCourseId(id)) {
            throw new CourseHasLessonsException();
        }
        courseRepository.delete(course);
        try {
            // flush để khóa ngoại RESTRICT báo lỗi ngay tại đây (không đợi đến lúc commit).
            courseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // Có bài vừa được thêm vào khóa này giữa lúc kiểm tra và lúc xóa; khóa ngoại là chốt cuối.
            throw new CourseHasLessonsException();
        }
    }

    private Course persist(Course course) {
        try {
            // saveAndFlush để ràng buộc UNIQUE của slug báo lỗi ngay tại đây (không đợi đến lúc commit).
            return courseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException e) {
            // Hai Admin dùng cùng slug cùng lúc: cả hai qua được kiểm tra trước, chỉ một người ghi được.
            throw new CourseSlugAlreadyExistsException();
        }
    }
}
