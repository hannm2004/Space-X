package vn.spacex.lesson;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.common.api.PageResponse;

// Chỉ ADMIN gọi được: luật "/api/admin/**" trong SecurityConfig, kể cả các GET (bài nháp không lộ cho học sinh).
// Người tạo bài lấy từ session (@AuthenticationPrincipal), không nhận từ trình duyệt.
@RestController
@RequestMapping("/api/admin/lessons")
public class AdminLessonController {

    private final AdminLessonService adminLessonService;

    public AdminLessonController(AdminLessonService adminLessonService) {
        this.adminLessonService = adminLessonService;
    }

    @GetMapping
    PageResponse<AdminLessonSummaryResponse> list(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) LessonStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return adminLessonService.list(courseId, status, page, size);
    }

    @GetMapping("/{id}")
    AdminLessonResponse get(@PathVariable Long id) {
        return adminLessonService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminLessonResponse create(@AuthenticationPrincipal AppUserDetails me,
                               @Valid @RequestBody AdminLessonRequest request) {
        return adminLessonService.create(me.id(), request);
    }

    @PutMapping("/{id}")
    AdminLessonResponse update(@PathVariable Long id, @Valid @RequestBody AdminLessonRequest request) {
        return adminLessonService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    AdminLessonResponse changeStatus(@PathVariable Long id, @Valid @RequestBody AdminLessonStatusRequest request) {
        return adminLessonService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        adminLessonService.delete(id);
    }
}
