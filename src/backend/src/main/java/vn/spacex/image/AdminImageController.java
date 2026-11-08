package vn.spacex.image;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// Chỉ ADMIN gọi được: luật "/api/admin/**" trong SecurityConfig.
@RestController
@RequestMapping("/api/admin")
public class AdminImageController {

    private final ImageService imageService;

    public AdminImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // Phần multipart tên "file". Tên tệp và Content-Type do trình duyệt gửi không được tin: ImageService nhận diện
    // loại ảnh từ nội dung.
    @PostMapping(path = "/lessons/{lessonId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ImageUploadResponse upload(@PathVariable Long lessonId, @RequestParam("file") MultipartFile file)
            throws IOException {
        // Ngoài giới hạn của spring.servlet.multipart, chặn sớm để không đọc cả tệp lớn vào bộ nhớ.
        if (file.getSize() > ImageService.MAX_BYTES) {
            throw new ImageTooLargeException();
        }
        return imageService.upload(lessonId, file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping("/lessons/{lessonId}/images")
    List<ImageResponse> list(@PathVariable Long lessonId) {
        return imageService.list(lessonId);
    }

    @DeleteMapping("/images/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        imageService.delete(id);
    }
}
