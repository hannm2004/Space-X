package vn.spacex.image;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

// Công khai (SecurityConfig cho GET /api/courses/*/cover): trang chủ hiện ảnh bìa khóa cho cả khách chưa đăng nhập.
@RestController
@RequestMapping("/api/courses")
public class CourseCoverController {

    private final CourseCoverService coverService;

    public CourseCoverController(CourseCoverService coverService) {
        this.coverService = coverService;
    }

    // Content-Type là loại do ứng dụng nhận diện lúc tải lên; Spring Security thêm X-Content-Type-Options: nosniff.
    // Cache "public" được vì ảnh không phụ thuộc người xem; đường dẫn có ?v= đổi khi ảnh bìa đổi.
    @GetMapping("/{slug}/cover")
    ResponseEntity<byte[]> view(@PathVariable String slug) {
        StoredImage image = coverService.load(slug);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(image.data());
    }
}
