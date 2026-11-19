package vn.spacex.image;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

// Gắn ảnh bìa vào danh sách bài công khai: một truy vấn cho cả danh sách (không hỏi từng bài).
@Component
public class CoverImageLookup {

    private final LessonImageRepository imageRepository;

    public CoverImageLookup(LessonImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    // Trả đường dẫn ảnh bìa theo mã bài; bài chưa có ảnh bìa không có trong kết quả.
    public Map<Long, String> urlsByLessonId(Collection<Long> lessonIds) {
        Map<Long, String> urls = new HashMap<>();
        if (lessonIds.isEmpty()) {
            return urls;
        }
        for (CoverRef ref : imageRepository.findCovers(lessonIds)) {
            urls.put(ref.getLessonId(), ImageService.urlOf(ref.getImageId()));
        }
        return urls;
    }
}
