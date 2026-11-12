package vn.spacex.progress;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.course.Course;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.course.CourseRepository;
import vn.spacex.lesson.Lesson;
import vn.spacex.lesson.LessonNotFoundException;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Mọi phương thức nhận userId do controller lấy từ session, không bao giờ từ dữ liệu trình duyệt gửi lên.
@Service
public class ProgressService {

    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final Clock clock;

    public ProgressService(LessonProgressRepository progressRepository, LessonRepository lessonRepository,
                           CourseRepository courseRepository, Clock clock) {
        this.progressRepository = progressRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.clock = clock;
    }

    // Cố ý không đặt @Transactional: mỗi lời gọi repository chạy trong transaction riêng, nên khi UNIQUE(user_id, lesson_id)
    // chặn một lần thêm trùng (hai yêu cầu đầu tiên đến cùng lúc), ngoại lệ bắt được ở đây và có thể đọc lại dòng vừa có.
    // Trong một transaction chung thì transaction đã bị đánh dấu hỏng, không thử lại được.
    public LessonProgressResponse record(Long userId, Long lessonId, ProgressStatus status) {
        // Bài nháp và bài không tồn tại cùng một lỗi: người gọi không dò được id nào là bài nháp.
        lessonRepository.findByIdAndStatus(lessonId, LessonStatus.PUBLISHED)
                .orElseThrow(LessonNotFoundException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            return apply(userId, lessonId, status, now);
        } catch (DataIntegrityViolationException racing) {
            return apply(userId, lessonId, status, now);
        }
    }

    private LessonProgressResponse apply(Long userId, Long lessonId, ProgressStatus status, LocalDateTime now) {
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> LessonProgress.start(userId, lessonId, now));
        if (status == ProgressStatus.COMPLETED) {
            progress.complete(now);
        } else {
            progress.view(now);
        }
        progressRepository.save(progress);
        return new LessonProgressResponse(lessonId, progress.getStatus());
    }

    // Chỉ tính bài Đã đăng: Admin ẩn một bài thì dòng tiến độ cũ của bài đó không còn được đếm.
    @Transactional(readOnly = true)
    public List<CourseProgressResponse> overview(Long userId) {
        Map<Long, List<Lesson>> lessonsByCourse = lessonRepository
                .findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED).stream()
                .collect(Collectors.groupingBy(Lesson::getCourseId));
        Map<Long, LessonProgress> progressByLesson = progressRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(LessonProgress::getLessonId, Function.identity()));

        return courseRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(course -> summarize(course, lessonsByCourse.getOrDefault(course.getId(), List.of()),
                        progressByLesson))
                .toList();
    }

    // "Học tiếp": bài đang học xem gần nhất; không có thì bài đầu tiên (theo thứ tự) chưa hoàn thành.
    private static CourseProgressResponse summarize(Course course, List<Lesson> lessons,
                                                    Map<Long, LessonProgress> progressByLesson) {
        int completed = 0;
        Lesson firstNotCompleted = null;
        Lesson latestInProgress = null;
        LocalDateTime latestViewedAt = null;
        for (Lesson lesson : lessons) {
            LessonProgress progress = progressByLesson.get(lesson.getId());
            if (progress != null && progress.getStatus() == ProgressStatus.COMPLETED) {
                completed++;
                continue;
            }
            if (firstNotCompleted == null) {
                firstNotCompleted = lesson;
            }
            if (progress != null && (latestViewedAt == null || progress.getLastViewedAt().isAfter(latestViewedAt))) {
                latestInProgress = lesson;
                latestViewedAt = progress.getLastViewedAt();
            }
        }
        Lesson next = latestInProgress != null ? latestInProgress : firstNotCompleted;
        int total = lessons.size();
        int percent = total == 0 ? 0 : completed * 100 / total;
        ContinueLessonResponse continueLesson = next == null
                ? null
                : new ContinueLessonResponse(next.getId(), next.getSlug(), next.getTitle());
        return new CourseProgressResponse(course.getId(), course.getSlug(), course.getTitle(),
                completed, total, percent, continueLesson);
    }

    @Transactional
    public void restartCourse(Long userId, Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException();
        }
        progressRepository.deleteByUserIdAndCourseId(userId, courseId);
    }
}
