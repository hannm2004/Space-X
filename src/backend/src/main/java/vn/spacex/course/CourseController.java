package vn.spacex.course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // duration (số giờ) và price (đồng) là khoảng "min-max"; sort là price_asc hoặc price_desc.
    @GetMapping
    List<CourseResponse> list(@RequestParam(required = false) String category,
                              @RequestParam(required = false) CourseLevel level,
                              @RequestParam(required = false) String duration,
                              @RequestParam(required = false) String price,
                              @RequestParam(required = false) String sort) {
        return courseService.listCourses(CourseFilter.of(category, level, duration, price, sort));
    }

    @GetMapping("/{slug}/lessons")
    List<LessonSummaryResponse> lessons(@PathVariable String slug) {
        return courseService.listPublishedLessons(slug);
    }
}
