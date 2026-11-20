package vn.spacex.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.spacex.course.Course;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.course.CourseRepository;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCoverServiceTest {

    private static final long COURSE_ID = 5L;
    private static final Instant NOW = Instant.parse("2026-11-19T03:00:00Z");

    @Mock
    private CourseCoverRepository coverRepository;

    @Mock
    private CourseRepository courseRepository;

    private CourseCoverService service;

    @BeforeEach
    void khoiTao() {
        service = new CourseCoverService(coverRepository, courseRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void coKhoa() {
        Course khoa = mock(Course.class);
        // lenient: các nhánh báo lỗi (ảnh quá lớn, sai loại, bỏ ảnh) không đọc slug.
        lenient().when(khoa.getSlug()).thenReturn("tin-hoc-10");
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(khoa));
    }

    // ---- Đặt ảnh bìa ----

    @Test
    void datAnhBiaLuuLoaiAnhThatVaTraDuongDanCoMocPhienBan() {
        coKhoa();
        byte[] png = ImageInspectorTest.png(10, 10);
        when(coverRepository.saveAndFlush(any(CourseCover.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCoverResponse response = service.setCover(COURSE_ID, png);

        ArgumentCaptor<CourseCover> saved = ArgumentCaptor.forClass(CourseCover.class);
        verify(coverRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCourseId()).isEqualTo(COURSE_ID);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(png.length);
        assertThat(saved.getValue().getData()).isEqualTo(png);
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        // ?v= đổi theo lần tải để trình duyệt không giữ ảnh bìa cũ.
        assertThat(response.coverImageUrl()).isEqualTo("/api/courses/tin-hoc-10/cover?v=" + NOW.toEpochMilli());
    }

    @Test
    void khoaKhongTonTaiNemCourseNotFoundVaKhongGhiGi() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setCover(COURSE_ID, ImageInspectorTest.png(10, 10)))
                .isInstanceOf(CourseNotFoundException.class);

        verifyNoInteractions(coverRepository);
    }

    @Test
    void anhLonHon1MBBiTuChoiVaKhongGhiGi() {
        coKhoa();

        assertThatThrownBy(() -> service.setCover(COURSE_ID, new byte[ImageService.MAX_BYTES + 1]))
                .isInstanceOf(ImageTooLargeException.class);

        verify(coverRepository, never()).saveAndFlush(any());
    }

    @Test
    void tepKhongPhaiAnhBiTuChoi() {
        coKhoa();
        byte[] vanBan = "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.setCover(COURSE_ID, vanBan))
                .isInstanceOf(UnsupportedImageTypeException.class);

        verify(coverRepository, never()).saveAndFlush(any());
    }

    // ---- Bỏ ảnh bìa ----

    @Test
    void boAnhBiaXoaDongCuaDungKhoa() {
        coKhoa();
        when(coverRepository.existsById(COURSE_ID)).thenReturn(true);

        service.clearCover(COURSE_ID);

        verify(coverRepository).deleteById(COURSE_ID);
    }

    @Test
    void boAnhBiaCuaKhoaChuaCoAnhKhongLoi() {
        coKhoa();
        when(coverRepository.existsById(COURSE_ID)).thenReturn(false);

        service.clearCover(COURSE_ID);

        verify(coverRepository, never()).deleteById(any());
    }

    @Test
    void boAnhBiaCuaKhoaKhongTonTaiNemCourseNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clearCover(COURSE_ID)).isInstanceOf(CourseNotFoundException.class);

        verifyNoInteractions(coverRepository);
    }

    // ---- Xem ảnh bìa (công khai) ----

    @Test
    void xemAnhBiaTraDungLoaiVaDuLieu() {
        Course khoa = mock(Course.class);
        when(khoa.getId()).thenReturn(COURSE_ID);
        when(courseRepository.findBySlug("tin-hoc-10")).thenReturn(Optional.of(khoa));
        byte[] png = ImageInspectorTest.png(4, 4);
        when(coverRepository.findById(COURSE_ID)).thenReturn(
                Optional.of(CourseCover.create(COURSE_ID, "image/png", png, LocalDateTime.of(2026, 11, 19, 3, 0))));

        StoredImage image = service.load("tin-hoc-10");

        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.data()).isEqualTo(png);
    }

    @Test
    void xemAnhBiaCuaKhoaKhongTonTaiNemCourseNotFound() {
        when(courseRepository.findBySlug("khong-co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.load("khong-co")).isInstanceOf(CourseNotFoundException.class);

        verifyNoInteractions(coverRepository);
    }

    @Test
    void xemAnhBiaCuaKhoaChuaCoAnhNemImageNotFound() {
        Course khoa = mock(Course.class);
        when(khoa.getId()).thenReturn(COURSE_ID);
        when(courseRepository.findBySlug("tin-hoc-10")).thenReturn(Optional.of(khoa));
        when(coverRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.load("tin-hoc-10")).isInstanceOf(ImageNotFoundException.class);
    }

    // ---- Gắn ảnh bìa vào danh sách khóa ----

    @Test
    void khoaCoAnhBiaCoDuongDanKhoaChuaCoAnhKhongCoTrongKetQua() {
        CourseCoverVersion bia = mock(CourseCoverVersion.class);
        when(bia.getCourseId()).thenReturn(5L);
        when(bia.getUpdatedAt()).thenReturn(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        when(coverRepository.findVersions(List.of(5L, 6L))).thenReturn(List.of(bia));
        // LinkedHashMap giữ thứ tự để lời gọi truy vấn khớp List.of(5L, 6L).
        Map<Long, String> slugs = new LinkedHashMap<>();
        slugs.put(5L, "tin-hoc-10");
        slugs.put(6L, "tin-hoc-11");

        Map<Long, String> ketQua = service.urlsByCourseId(slugs);

        assertThat(ketQua).containsOnly(Map.entry(5L, "/api/courses/tin-hoc-10/cover?v=" + NOW.toEpochMilli()));
    }

    @Test
    void urlOfCourseTraDuongDanKhiKhoaCoAnhVaNullKhiChuaCo() {
        CourseCoverVersion bia = mock(CourseCoverVersion.class);
        when(bia.getCourseId()).thenReturn(5L);
        when(bia.getUpdatedAt()).thenReturn(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        when(coverRepository.findVersions(List.of(5L))).thenReturn(List.of(bia));
        when(coverRepository.findVersions(List.of(6L))).thenReturn(List.of());

        assertThat(service.urlOfCourse(5L, "tin-hoc-10"))
                .isEqualTo("/api/courses/tin-hoc-10/cover?v=" + NOW.toEpochMilli());
        assertThat(service.urlOfCourse(6L, "tin-hoc-11")).isNull();
    }

    @Test
    void khongCoKhoaNaoThiKhongHoiDatabase() {
        assertThat(service.urlsByCourseId(Map.of())).isEmpty();

        verifyNoInteractions(coverRepository);
    }
}
