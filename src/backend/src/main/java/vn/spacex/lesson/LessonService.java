package vn.spacex.lesson;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonService {

    private final LessonContentRepository lessonContentRepository;

    public LessonService(LessonContentRepository lessonContentRepository) {
        this.lessonContentRepository = lessonContentRepository;
    }

    // Trạng thái PUBLISHED nằm ngay trong truy vấn nên bài nháp không bao giờ được nạp lên.
    @Transactional(readOnly = true)
    public LessonDetailResponse getPublishedLesson(String slug) {
        return lessonContentRepository.findBySlugAndStatus(slug, LessonStatus.PUBLISHED)
                .map(LessonDetailResponse::from)
                .orElseThrow(LessonNotFoundException::new);
    }
}
