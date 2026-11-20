package vn.spacex.image;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

// Chỉ ADMIN gọi được: luật "/api/admin/**" trong SecurityConfig.
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseCoverController {

    private final CourseCoverService coverService;

    public AdminCourseCoverController(CourseCoverService coverService) {
        this.coverService = coverService;
    }

    // Phần multipart tên "file". Tên tệp và Content-Type do trình duyệt gửi không được tin: CourseCoverService nhận diện
    // loại ảnh từ nội dung. POST (như tải ảnh bài học): đặt ảnh mới thay ảnh cũ của khóa.
    @PostMapping(path = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    CourseCoverResponse set(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        // Ngoài giới hạn của spring.servlet.multipart, chặn sớm để không đọc cả tệp lớn vào bộ nhớ.
        if (file.getSize() > ImageService.MAX_BYTES) {
            throw new ImageTooLargeException();
        }
        return coverService.setCover(id, file.getBytes());
    }

    @DeleteMapping("/{id}/cover")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clear(@PathVariable Long id) {
        coverService.clearCover(id);
    }
}
