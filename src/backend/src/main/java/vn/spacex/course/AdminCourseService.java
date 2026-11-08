package vn.spacex.course;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.lesson.LessonRepository;

@Service
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    public AdminCourseService(CourseRepository courseRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional
    public AdminCourseResponse create(AdminCourseRequest request) {
        if (courseRepository.existsBySlug(request.slug())) {
            throw new CourseSlugAlreadyExistsException();
        }
        Course course = Course.create(request.slug(), request.title(), request.description(), request.sortOrder());
        return save(course);
    }

    @Transactional
    public AdminCourseResponse update(Long id, AdminCourseRequest request) {
        Course course = courseRepository.findById(id).orElseThrow(CourseNotFoundException::new);
        if (courseRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new CourseSlugAlreadyExistsException();
        }
        course.update(request.slug(), request.title(), request.description(), request.sortOrder());
        return save(course);
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

    private AdminCourseResponse save(Course course) {
        try {
            // saveAndFlush để ràng buộc UNIQUE của slug báo lỗi ngay tại đây (không đợi đến lúc commit).
            return AdminCourseResponse.from(courseRepository.saveAndFlush(course));
        } catch (DataIntegrityViolationException e) {
            // Hai Admin dùng cùng slug cùng lúc: cả hai qua được kiểm tra trước, chỉ một người ghi được.
            throw new CourseSlugAlreadyExistsException();
        }
    }
}
