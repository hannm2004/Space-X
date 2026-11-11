package vn.spacex.lesson;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.common.api.PageResponse;

@Service
public class LessonService {

    private final LessonContentRepository lessonContentRepository;
    private final LessonRepository lessonRepository;

    public LessonService(LessonContentRepository lessonContentRepository, LessonRepository lessonRepository) {
        this.lessonContentRepository = lessonContentRepository;
        this.lessonRepository = lessonRepository;
    }

    // Truy vấn tự sắp theo độ liên quan nên PageRequest không có Sort. Danh sách dùng entity Lesson (không có content_md).
    @Transactional(readOnly = true)
    public PageResponse<LessonSummaryResponse> search(String query, int page, int size) {
        return PageResponse.of(lessonRepository.searchPublished(query.strip(), PageRequest.of(page, size)),
                LessonSummaryResponse::from);
    }

    // Trạng thái PUBLISHED nằm ngay trong truy vấn nên bài nháp không bao giờ được nạp lên.
    @Transactional(readOnly = true)
    public LessonDetailResponse getPublishedLesson(String slug) {
        return lessonContentRepository.findBySlugAndStatus(slug, LessonStatus.PUBLISHED)
                .map(LessonDetailResponse::from)
                .orElseThrow(LessonNotFoundException::new);
    }
}
