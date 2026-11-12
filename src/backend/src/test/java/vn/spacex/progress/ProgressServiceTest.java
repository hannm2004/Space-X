package vn.spacex.progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import vn.spacex.course.Course;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.course.CourseRepository;
import vn.spacex.lesson.Lesson;
import vn.spacex.lesson.LessonNotFoundException;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final long USER_ID = 7L;
    private static final long LESSON_ID = 11L;
    private static final long COURSE_ID = 3L;

    private static final LocalDateTime BAY_GIO = LocalDateTime.of(2026, 11, 12, 10, 0);
    private static final LocalDateTime TRUOC_DO = LocalDateTime.of(2026, 11, 12, 8, 0);

    @Mock
    private LessonProgressRepository progressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    private final Clock dongHo = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);

    private ProgressService service() {
        return new ProgressService(progressRepository, lessonRepository, courseRepository, dongHo);
    }

    // Lớp con thay cho mock: gọi bai(...) bên trong thenReturn(...) của một when(...) khác sẽ làm Mockito báo stubbing lồng nhau.
    private static Lesson bai(long id, long courseId, String slug, String title) {
        return new Lesson() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public Long getCourseId() {
                return courseId;
            }

            @Override
            public String getSlug() {
                return slug;
            }

            @Override
            public String getTitle() {
                return title;
            }
        };
    }

    private static Course khoa(long id, String slug, String title) {
        Course khoa = Course.create(slug, title, null, 0);
        ReflectionTestUtils.setField(khoa, "id", id);
        return khoa;
    }

    private static LessonProgress dangHoc(long lessonId, LocalDateTime xemLuc) {
        return LessonProgress.start(USER_ID, lessonId, xemLuc);
    }

    private static LessonProgress daXong(long lessonId, LocalDateTime xongLuc) {
        LessonProgress tienDo = LessonProgress.start(USER_ID, lessonId, xongLuc);
        tienDo.complete(xongLuc);
        return tienDo;
    }

    // ---- Ghi nhận tiến độ một bài ----

    @Test
    void baiKhongTonTaiHoacChuaDangNemLessonNotFoundVaKhongGhiGi() {
        when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().record(USER_ID, LESSON_ID, ProgressStatus.COMPLETED))
                .isInstanceOf(LessonNotFoundException.class);

        verify(progressRepository, never()).save(any());
    }

    @Test
    void moBaiLanDauTaoDongInProgressChoDungNguoiDung() {
        when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                .thenReturn(Optional.of(bai(LESSON_ID, COURSE_ID, "may-tinh", "Máy tính")));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.empty());

        LessonProgressResponse ketQua = service().record(USER_ID, LESSON_ID, ProgressStatus.IN_PROGRESS);

        ArgumentCaptor<LessonProgress> daLuu = ArgumentCaptor.forClass(LessonProgress.class);
        verify(progressRepository).save(daLuu.capture());
        assertThat(daLuu.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(daLuu.getValue().getLessonId()).isEqualTo(LESSON_ID);
        assertThat(daLuu.getValue().getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(daLuu.getValue().getLastViewedAt()).isEqualTo(BAY_GIO);
        assertThat(daLuu.getValue().getCompletedAt()).isNull();
        assertThat(ketQua).isEqualTo(new LessonProgressResponse(LESSON_ID, ProgressStatus.IN_PROGRESS));
    }

    @Test
    void moBaiLanNuaChiCapNhatLastViewedAtVaKhongTaoDongMoi() {
        LessonProgress cu = dangHoc(LESSON_ID, TRUOC_DO);
        when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                .thenReturn(Optional.of(bai(LESSON_ID, COURSE_ID, "may-tinh", "Máy tính")));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(cu));

        service().record(USER_ID, LESSON_ID, ProgressStatus.IN_PROGRESS);

        ArgumentCaptor<LessonProgress> daLuu = ArgumentCaptor.forClass(LessonProgress.class);
        verify(progressRepository).save(daLuu.capture());
        assertThat(daLuu.getValue()).isSameAs(cu);
        assertThat(cu.getLastViewedAt()).isEqualTo(BAY_GIO);
        assertThat(cu.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void bamHoanThanhBaiGhiCompletedVaThoiDiemHoanThanh() {
        LessonProgress cu = dangHoc(LESSON_ID, TRUOC_DO);
        when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                .thenReturn(Optional.of(bai(LESSON_ID, COURSE_ID, "may-tinh", "Máy tính")));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(cu));

        LessonProgressResponse ketQua = service().record(USER_ID, LESSON_ID, ProgressStatus.COMPLETED);

        assertThat(cu.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(cu.getCompletedAt()).isEqualTo(BAY_GIO);
        assertThat(ketQua.status()).isEqualTo(ProgressStatus.COMPLETED);
    }

    // Quy tắc ở docs/DATABASE_DESIGN.md: bài đã COMPLETED không bị hạ về IN_PROGRESS khi mở lại.
    @Test
    void baiDaHoanThanhMoLaiKhongBiHaVeInProgressVaVanBaoCompleted() {
        LessonProgress cu = daXong(LESSON_ID, TRUOC_DO);
        when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                .thenReturn(Optional.of(bai(LESSON_ID, COURSE_ID, "may-tinh", "Máy tính")));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(cu));

        LessonProgressResponse ketQua = service().record(USER_ID, LESSON_ID, ProgressStatus.IN_PROGRESS);

        assertThat(cu.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(cu.getCompletedAt()).isEqualTo(TRUOC_DO);
        assertThat(cu.getLastViewedAt()).isEqualTo(BAY_GIO);
        assertThat(ketQua.status()).isEqualTo(ProgressStatus.COMPLETED);
    }

    @Test
    void bamHoanThanhHaiLanGiuNguyenThoiDiemHoanThanhLanDau() {
        LessonProgress cu = daXong(LESSON_ID, TRUOC_DO);
        when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                .thenReturn(Optional.of(bai(LESSON_ID, COURSE_ID, "may-tinh", "Máy tính")));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(cu));

        service().record(USER_ID, LESSON_ID, ProgressStatus.COMPLETED);

        assertThat(cu.getCompletedAt()).isEqualTo(TRUOC_DO);
    }

    // Hai yêu cầu đầu tiên đến cùng lúc (bấm đúp, hai tab): cả hai thấy chưa có dòng và cùng thêm.
    // UNIQUE(user_id, lesson_id) chặn một bên; bên đó phải đọc lại dòng đã có và cập nhật, không trả lỗi 500.
    @Test
    void haiYeuCauDauTienCungLucThiBenThuaDocLaiDongDaCoVaCapNhat() {
        LessonProgress doBenKia = dangHoc(LESSON_ID, TRUOC_DO);
        when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                .thenReturn(Optional.of(bai(LESSON_ID, COURSE_ID, "may-tinh", "Máy tính")));
        when(progressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(doBenKia));
        when(progressRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_progress_user_lesson"))
                .thenAnswer(lanGoi -> lanGoi.getArgument(0));

        LessonProgressResponse ketQua = service().record(USER_ID, LESSON_ID, ProgressStatus.COMPLETED);

        verify(progressRepository, times(2)).save(any());
        assertThat(doBenKia.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(ketQua.status()).isEqualTo(ProgressStatus.COMPLETED);
    }

    // ---- Tiến độ từng khóa ----

    @Test
    void chuaCoKhoaNaoThiTraVeMangRong() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of());
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThat(service().overview(USER_ID)).isEmpty();
    }

    @Test
    void phanTramLaSoBaiHoanThanhTrenTongBaiDaDangLamTronXuongVaChiMuoiMotKhiXongHet() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(khoa(COURSE_ID, "tin-hoc-10", "Tin học 10")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of(
                bai(1, COURSE_ID, "a", "A"), bai(2, COURSE_ID, "b", "B"), bai(3, COURSE_ID, "c", "C")));
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of(daXong(1, TRUOC_DO)));

        CourseProgressResponse khoa10 = service().overview(USER_ID).get(0);

        assertThat(khoa10.courseId()).isEqualTo(COURSE_ID);
        assertThat(khoa10.courseSlug()).isEqualTo("tin-hoc-10");
        assertThat(khoa10.courseTitle()).isEqualTo("Tin học 10");
        assertThat(khoa10.completedLessons()).isEqualTo(1);
        assertThat(khoa10.totalLessons()).isEqualTo(3);
        assertThat(khoa10.percent()).isEqualTo(33);
    }

    // Admin ẩn bài thì bài không còn trong danh sách bài Đã đăng, nên dòng tiến độ cũ của bài đó không được tính.
    @Test
    void baiDaBiAnVanConDongTienDoKhongDuocTinhVaoPhanTram() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(khoa(COURSE_ID, "tin-hoc-10", "Tin học 10")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of(
                bai(1, COURSE_ID, "a", "A"), bai(2, COURSE_ID, "b", "B")));
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of(daXong(1, TRUOC_DO), daXong(99, TRUOC_DO)));

        CourseProgressResponse khoa10 = service().overview(USER_ID).get(0);

        assertThat(khoa10.completedLessons()).isEqualTo(1);
        assertThat(khoa10.totalLessons()).isEqualTo(2);
        assertThat(khoa10.percent()).isEqualTo(50);
    }

    @Test
    void hocTiepLaBaiDangHocMoiXemGanNhatDuBaiKhacCoThuTuTruocHon() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(khoa(COURSE_ID, "tin-hoc-10", "Tin học 10")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of(
                bai(1, COURSE_ID, "a", "A"), bai(2, COURSE_ID, "b", "B"), bai(3, COURSE_ID, "c", "C")));
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of(
                dangHoc(1, TRUOC_DO), dangHoc(3, BAY_GIO)));

        CourseProgressResponse khoa10 = service().overview(USER_ID).get(0);

        assertThat(khoa10.continueLesson()).isEqualTo(new ContinueLessonResponse(3L, "c", "C"));
    }

    @Test
    void khongCoBaiDangHocThiHocTiepLaBaiDauTienChuaHoanThanhTheoThuTu() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(khoa(COURSE_ID, "tin-hoc-10", "Tin học 10")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of(
                bai(1, COURSE_ID, "a", "A"), bai(2, COURSE_ID, "b", "B"), bai(3, COURSE_ID, "c", "C")));
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of(daXong(1, TRUOC_DO)));

        CourseProgressResponse khoa10 = service().overview(USER_ID).get(0);

        assertThat(khoa10.continueLesson()).isEqualTo(new ContinueLessonResponse(2L, "b", "B"));
    }

    @Test
    void chuaHocGiThiHocTiepLaBaiDauTienCuaKhoa() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(khoa(COURSE_ID, "tin-hoc-10", "Tin học 10")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of(
                bai(1, COURSE_ID, "a", "A"), bai(2, COURSE_ID, "b", "B")));
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of());

        CourseProgressResponse khoa10 = service().overview(USER_ID).get(0);

        assertThat(khoa10.percent()).isZero();
        assertThat(khoa10.continueLesson()).isEqualTo(new ContinueLessonResponse(1L, "a", "A"));
    }

    @Test
    void hocXongHetThiMotTramPhanTramVaKhongConHocTiep() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(khoa(COURSE_ID, "tin-hoc-10", "Tin học 10")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of(
                bai(1, COURSE_ID, "a", "A"), bai(2, COURSE_ID, "b", "B")));
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of(daXong(1, TRUOC_DO), daXong(2, BAY_GIO)));

        CourseProgressResponse khoa10 = service().overview(USER_ID).get(0);

        assertThat(khoa10.percent()).isEqualTo(100);
        assertThat(khoa10.continueLesson()).isNull();
    }

    @Test
    void khoaChuaCoBaiDaDangThiPhanTramLaKhongVaKhongChiaChoKhong() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(khoa(COURSE_ID, "tin-hoc-12", "Tin học 12")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of());
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of());

        CourseProgressResponse khoa12 = service().overview(USER_ID).get(0);

        assertThat(khoa12.totalLessons()).isZero();
        assertThat(khoa12.completedLessons()).isZero();
        assertThat(khoa12.percent()).isZero();
        assertThat(khoa12.continueLesson()).isNull();
    }

    @Test
    void tienDoMoiKhoaChiTinhTrenBaiCuaKhoaDoVaGiuThuTuKhoa() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                khoa(1, "tin-hoc-10", "Tin học 10"), khoa(2, "tin-hoc-11", "Tin học 11")));
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of(
                bai(1, 1, "a", "A"), bai(2, 1, "b", "B"), bai(3, 2, "c", "C")));
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of(daXong(3, TRUOC_DO)));

        List<CourseProgressResponse> ketQua = service().overview(USER_ID);

        assertThat(ketQua).extracting(CourseProgressResponse::courseSlug).containsExactly("tin-hoc-10", "tin-hoc-11");
        assertThat(ketQua.get(0).completedLessons()).isZero();
        assertThat(ketQua.get(0).totalLessons()).isEqualTo(2);
        assertThat(ketQua.get(1).completedLessons()).isEqualTo(1);
        assertThat(ketQua.get(1).percent()).isEqualTo(100);
    }

    @Test
    void chiDocTienDoCuaNguoiDungDuocTruyenVaChiHoiBaiDaDang() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());
        when(lessonRepository.findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED)).thenReturn(List.of());
        when(progressRepository.findByUserId(USER_ID)).thenReturn(List.of());

        service().overview(USER_ID);

        verify(progressRepository).findByUserId(USER_ID);
        verify(lessonRepository).findByStatusOrderBySortOrderAscIdAsc(LessonStatus.PUBLISHED);
    }

    // ---- Học lại khóa ----

    @Test
    void hocLaiKhoaKhongTonTaiNemCourseNotFoundVaKhongXoaGi() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service().restartCourse(USER_ID, COURSE_ID))
                .isInstanceOf(CourseNotFoundException.class);

        verify(progressRepository, never()).deleteByUserIdAndCourseId(anyLong(), anyLong());
    }

    @Test
    void hocLaiKhoaXoaTienDoCuaDungNguoiDungTrongDungKhoa() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);

        service().restartCourse(USER_ID, COURSE_ID);

        verify(progressRepository).deleteByUserIdAndCourseId(USER_ID, COURSE_ID);
    }
}
