package vn.spacex.lesson;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Không có luật riêng trong SecurityConfig: "/api/**" đã đòi đăng nhập nên đọc nội dung bài cần session.
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping("/{slug}")
    LessonDetailResponse detail(@PathVariable String slug) {
        return lessonService.getPublishedLesson(slug);
    }
}
