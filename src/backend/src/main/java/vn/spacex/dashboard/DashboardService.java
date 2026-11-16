package vn.spacex.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.course.CourseRepository;
import vn.spacex.lesson.LessonCountByCourse;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;
import vn.spacex.progress.LessonProgressRepository;
import vn.spacex.progress.ProgressStatus;
import vn.spacex.user.Role;
import vn.spacex.user.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int NEW_USER_DAYS = 7;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final Clock clock;

    public DashboardService(UserRepository userRepository, CourseRepository courseRepository,
                            LessonRepository lessonRepository, LessonProgressRepository progressRepository,
                            Clock clock) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(NEW_USER_DAYS);
        DashboardResponse.UserStats users = new DashboardResponse.UserStats(
                userRepository.count(), userRepository.countByCreatedAtGreaterThanEqual(since));

        long draft = lessonRepository.countByStatus(LessonStatus.DRAFT);
        long published = lessonRepository.countByStatus(LessonStatus.PUBLISHED);
        DashboardResponse.LessonStats lessons = new DashboardResponse.LessonStats(draft + published, draft, published);

        DashboardResponse.LearningStats learning = new DashboardResponse.LearningStats(
                progressRepository.countDistinctUsersByRole(Role.LEARNER),
                progressRepository.countByStatusAndUserRole(ProgressStatus.COMPLETED, Role.LEARNER));

        return new DashboardResponse(users, lessons, courseStats(), learning);
    }

    // Đi từ danh sách khóa (không phải từ kết quả nhóm) để khóa chưa có bài vẫn xuất hiện, đúng thứ tự hiển thị.
    private List<DashboardResponse.CourseStats> courseStats() {
        Map<Long, Map<LessonStatus, Long>> countsByCourse = new HashMap<>();
        for (LessonCountByCourse row : lessonRepository.countLessonsByCourseAndStatus()) {
            countsByCourse.computeIfAbsent(row.getCourseId(), id -> new HashMap<>()).put(row.getStatus(), row.getTotal());
        }
        return courseRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(course -> {
                    Map<LessonStatus, Long> counts = countsByCourse.getOrDefault(course.getId(), Map.of());
                    return new DashboardResponse.CourseStats(course.getId(), course.getSlug(), course.getTitle(),
                            counts.getOrDefault(LessonStatus.DRAFT, 0L),
                            counts.getOrDefault(LessonStatus.PUBLISHED, 0L));
                })
                .toList();
    }
}
