package vn.spacex.lesson;

import java.util.List;
import java.util.Map;
import vn.spacex.image.CoverImageLookup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.common.api.PageResponse;

@Service
public class LessonService {

    private final LessonContentRepository lessonContentRepository;
    private final LessonRepository lessonRepository;
    private final CoverImageLookup coverImageLookup;

    public LessonService(LessonContentRepository lessonContentRepository, LessonRepository lessonRepository,
                         CoverImageLookup coverImageLookup) {
        this.lessonContentRepository = lessonContentRepository;
        this.lessonRepository = lessonRepository;
        this.coverImageLookup = coverImageLookup;
    }

    // Truy vấn tự sắp theo độ liên quan nên PageRequest không có Sort. Danh sách dùng entity Lesson (không có content_md).
    @Transactional(readOnly = true)
    public PageResponse<LessonSummaryResponse> search(String query, int page, int size) {
        Page<Lesson> found = lessonRepository.searchPublished(query.strip(), PageRequest.of(page, size));
        Map<Long, String> covers = coverImageLookup.urlsByLessonId(found.getContent().stream().map(Lesson::getId).toList());
        return PageResponse.of(found, lesson -> LessonSummaryResponse.from(lesson, covers.get(lesson.getId())));
    }

    // Trạng thái PUBLISHED nằm ngay trong truy vấn nên bài nháp không bao giờ được nạp lên.
    @Transactional(readOnly = true)
    public LessonDetailResponse getPublishedLesson(String slug) {
        return lessonContentRepository.findBySlugAndStatus(slug, LessonStatus.PUBLISHED)
                .map(lesson -> LessonDetailResponse.from(lesson,
                        coverImageLookup.urlsByLessonId(List.of(lesson.getId())).get(lesson.getId())))
                .orElseThrow(LessonNotFoundException::new);
    }
}
