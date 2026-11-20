package vn.spacex.lesson;

import java.util.List;
import java.util.Map;
import vn.spacex.image.CoverImageLookup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.common.api.PageResponse;
import vn.spacex.course.Course;
import vn.spacex.course.CourseRepository;

@Service
public class LessonService {

    private final LessonContentRepository lessonContentRepository;
    private final LessonRepository lessonRepository;
    private final CoverImageLookup coverImageLookup;
    private final CourseRepository courseRepository;

    public LessonService(LessonContentRepository lessonContentRepository, LessonRepository lessonRepository,
                         CoverImageLookup coverImageLookup, CourseRepository courseRepository) {
        this.lessonContentRepository = lessonContentRepository;
        this.lessonRepository = lessonRepository;
        this.coverImageLookup = coverImageLookup;
        this.courseRepository = courseRepository;
    }

    // Truy vấn tự sắp theo độ liên quan nên PageRequest không có Sort. Danh sách dùng entity Lesson (không có content_md).
    @Transactional(readOnly = true)
    public PageResponse<LessonSummaryResponse> search(String query, int page, int size) {
        Page<Lesson> found = lessonRepository.searchPublished(query.strip(), PageRequest.of(page, size));
        Map<Long, String> covers = coverImageLookup.urlsByLessonId(found.getContent().stream().map(Lesson::getId).toList());
        return PageResponse.of(found, lesson -> LessonSummaryResponse.from(lesson, covers.get(lesson.getId())));
    }

    // Trạng thái PUBLISHED nằm ngay trong truy vấn nên bài nháp không bao giờ được nạp lên.
    // Bài trước/sau lấy từ các bài đã đăng cùng khóa theo đúng thứ tự trang khóa học (sort_order rồi id), nên bài nháp
    // nằm giữa hai bài đã đăng bị bỏ qua, và bài của khóa khác không lẫn vào.
    @Transactional(readOnly = true)
    public LessonDetailResponse getPublishedLesson(String slug) {
        LessonContent lesson = lessonContentRepository.findBySlugAndStatus(slug, LessonStatus.PUBLISHED)
                .orElseThrow(LessonNotFoundException::new);
        // Khóa luôn có vì lessons.course_id là khóa ngoại; thiếu nghĩa là dữ liệu hỏng, nên để lỗi 500 chứ không che đi.
        Course course = courseRepository.findById(lesson.getCourseId())
                .orElseThrow(() -> new IllegalStateException("Bài học trỏ tới khóa không tồn tại"));
        List<Lesson> siblings = lessonRepository
                .findByCourseIdAndStatusOrderBySortOrderAscIdAsc(lesson.getCourseId(), LessonStatus.PUBLISHED);
        int index = indexOf(siblings, lesson.getId());
        ContentRef previous = index > 0 ? refOf(siblings.get(index - 1)) : null;
        ContentRef next = index >= 0 && index < siblings.size() - 1 ? refOf(siblings.get(index + 1)) : null;
        String cover = coverImageLookup.urlsByLessonId(List.of(lesson.getId())).get(lesson.getId());
        return LessonDetailResponse.from(lesson, cover, new ContentRef(course.getSlug(), course.getTitle()),
                previous, next);
    }

    private static int indexOf(List<Lesson> lessons, Long id) {
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static ContentRef refOf(Lesson lesson) {
        return new ContentRef(lesson.getSlug(), lesson.getTitle());
    }
}
