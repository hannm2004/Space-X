package vn.spacex.course;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Chỉ ADMIN gọi được: luật "/api/admin/**" trong SecurityConfig, không cần kiểm lại ở đây.
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminCourseResponse create(@Valid @RequestBody AdminCourseRequest request) {
        return adminCourseService.create(request);
    }

    @PutMapping("/{id}")
    AdminCourseResponse update(@PathVariable Long id, @Valid @RequestBody AdminCourseRequest request) {
        return adminCourseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        adminCourseService.delete(id);
    }
}
