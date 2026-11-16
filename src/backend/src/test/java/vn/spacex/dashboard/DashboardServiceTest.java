package vn.spacex.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.spacex.course.Course;
import vn.spacex.course.CourseRepository;
import vn.spacex.lesson.LessonCountByCourse;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;
import vn.spacex.progress.LessonProgressRepository;
import vn.spacex.progress.ProgressStatus;
import vn.spacex.user.Role;
import vn.spacex.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
    // 2026-11-16 10:00 giờ Việt Nam; ngưỡng "7 ngày qua" phải là đúng 2026-11-09 10:00.
    private static final Clock DONG_HO = Clock.fixed(Instant.parse("2026-11-16T03:00:00Z"), MUI_GIO);

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository progressRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(userRepository, courseRepository, lessonRepository, progressRepository,
                DONG_HO);
    }

    private static Course khoa(long id, String slug, String title) {
        Course course = Course.create(slug, title, null, 0);
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private static LessonCountByCourse dem(long courseId, LessonStatus status, long total) {
        return new LessonCountByCourse() {
            @Override
            public Long getCourseId() {
                return courseId;
            }

            @Override
            public LessonStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void demNguoiDungMoiTinhTuDungBayNgayTruocGioHienTai() {
        when(userRepository.count()).thenReturn(12L);
        when(userRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(3L);
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        DashboardResponse response = service.summary();

        assertThat(response.users().total()).isEqualTo(12);
        assertThat(response.users().newLast7Days()).isEqualTo(3);
        ArgumentCaptor<LocalDateTime> nguong = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).countByCreatedAtGreaterThanEqual(nguong.capture());
        assertThat(nguong.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 9, 10, 0));
    }

    @Test
    void demBaiTheoTrangThaiVaTongBai() {
        when(lessonRepository.countByStatus(LessonStatus.DRAFT)).thenReturn(2L);
        when(lessonRepository.countByStatus(LessonStatus.PUBLISHED)).thenReturn(5L);
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        DashboardResponse response = service.summary();

        assertThat(response.lessons().draft()).isEqualTo(2);
        assertThat(response.lessons().published()).isEqualTo(5);
        assertThat(response.lessons().total()).isEqualTo(7);
    }

    @Test
    void soBaiTheoKhoaGopDungNhapVaDaDangVaGiuThuTuKhoa() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(khoa(1, "tin-hoc-10", "Tin học 10"), khoa(2, "tin-hoc-11", "Tin học 11")));
        when(lessonRepository.countLessonsByCourseAndStatus()).thenReturn(List.of(
                dem(2, LessonStatus.PUBLISHED, 4),
                dem(1, LessonStatus.DRAFT, 1),
                dem(1, LessonStatus.PUBLISHED, 3)));

        List<DashboardResponse.CourseStats> courses = service.summary().courses();

        assertThat(courses).extracting(DashboardResponse.CourseStats::slug)
                .containsExactly("tin-hoc-10", "tin-hoc-11");
        assertThat(courses.get(0).draft()).isEqualTo(1);
        assertThat(courses.get(0).published()).isEqualTo(3);
        assertThat(courses.get(1).draft()).isZero();
        assertThat(courses.get(1).published()).isEqualTo(4);
    }

    @Test
    void khoaChuaCoBaiVanXuatHienVoiSoKhong() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(khoa(9, "tin-hoc-12", "Tin học 12")));
        when(lessonRepository.countLessonsByCourseAndStatus()).thenReturn(List.of());

        List<DashboardResponse.CourseStats> courses = service.summary().courses();

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).courseId()).isEqualTo(9);
        assertThat(courses.get(0).title()).isEqualTo("Tin học 12");
        assertThat(courses.get(0).draft()).isZero();
        assertThat(courses.get(0).published()).isZero();
    }

    @Test
    void tienDoChiTinhHocSinhKhongTinhQuanTriVien() {
        when(progressRepository.countDistinctUsersByRole(Role.LEARNER)).thenReturn(4L);
        when(progressRepository.countByStatusAndUserRole(ProgressStatus.COMPLETED, Role.LEARNER)).thenReturn(9L);
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        DashboardResponse response = service.summary();

        assertThat(response.learning().activeLearners()).isEqualTo(4);
        assertThat(response.learning().completedLessons()).isEqualTo(9);
    }

    @Test
    void chuaCoDuLieuThiMoiSoDeuBangKhong() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        DashboardResponse response = service.summary();

        assertThat(response.users().total()).isZero();
        assertThat(response.users().newLast7Days()).isZero();
        assertThat(response.lessons().total()).isZero();
        assertThat(response.courses()).isEmpty();
        assertThat(response.learning().activeLearners()).isZero();
        assertThat(response.learning().completedLessons()).isZero();
    }
}
