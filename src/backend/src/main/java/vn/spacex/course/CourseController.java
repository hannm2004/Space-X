package vn.spacex.course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.lesson.LessonSummaryResponse;

import java.util.List;

// Hai đường dẫn GET này công khai (SecurityConfig); mọi đường dẫn khác dưới /api/courses vẫn đòi đăng nhập.
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    List<CourseResponse> list() {
        return courseService.listCourses();
    }

    @GetMapping("/{slug}/lessons")
    List<LessonSummaryResponse> lessons(@PathVariable String slug) {
        return courseService.listPublishedLessons(slug);
    }
}
