package vn.spacex.lesson;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.access.Reader;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.common.api.PageResponse;

// Không có luật riêng trong SecurityConfig: "/api/**" đã đòi đăng nhập nên đọc nội dung bài cần session.
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    // Tìm trong các bài người gọi được đọc (D29, D40). Giới hạn như API Admin: size 1-50, trang đếm từ 0; từ khóa
    // 1-200 ký tự, không toàn khoảng trắng.
    @GetMapping
    PageResponse<LessonSummaryResponse> search(
            @AuthenticationPrincipal AppUserDetails me,
            @RequestParam @NotBlank @Size(max = 200) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return lessonService.search(q, Reader.of(me), page, size);
    }

    @GetMapping("/{slug}")
    LessonDetailResponse detail(@AuthenticationPrincipal AppUserDetails me, @PathVariable String slug) {
        return lessonService.getLesson(slug, Reader.of(me));
    }
}
