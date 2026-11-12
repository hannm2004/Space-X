package vn.spacex.progress;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.auth.AppUserDetails;

import java.util.List;

// Người dùng hiện tại lấy từ session (@AuthenticationPrincipal), không nhận userId từ trình duyệt: đường dẫn chỉ có
// lessonId/courseId nên không có chỗ nào để chỉ sang tiến độ của người khác. /api/** đã đòi đăng nhập ở SecurityConfig.
@RestController
@RequestMapping("/api/me/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    List<CourseProgressResponse> overview(@AuthenticationPrincipal AppUserDetails me) {
        return progressService.overview(me.id());
    }

    // PUT riêng thay vì ghi ngầm trong GET bài học: GET phải an toàn (docs/API.md).
    @PutMapping("/lessons/{lessonId}")
    LessonProgressResponse record(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long lessonId,
                                  @Valid @RequestBody ProgressRequest body) {
        return progressService.record(me.id(), lessonId, body.status());
    }

    @DeleteMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void restartCourse(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long courseId) {
        progressService.restartCourse(me.id(), courseId);
    }
}
