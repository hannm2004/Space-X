package vn.spacex.course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Công khai (SecurityConfig): menu nhóm chương trình và ô chọn nhóm ở trang quản trị.
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CourseService courseService;

    public CategoryController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    List<CategoryResponse> list() {
        return courseService.listCategories();
    }
}
