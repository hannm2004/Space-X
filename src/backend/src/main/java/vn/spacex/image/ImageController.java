package vn.spacex.image;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.user.Role;

import java.util.concurrent.TimeUnit;

// Không có luật riêng trong SecurityConfig: "/api/**" đã đòi đăng nhập. Quyền xem theo trạng thái bài nằm ở ImageService.
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // Content-Type là loại do ứng dụng nhận diện lúc tải lên; Spring Security thêm X-Content-Type-Options: nosniff.
    // Cache "private": ảnh sau kiểm quyền không được lưu ở proxy dùng chung.
    @GetMapping("/{id}")
    ResponseEntity<byte[]> view(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id) {
        StoredImage image = imageService.load(id, me.role() == Role.ADMIN);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(image.data());
    }
}
