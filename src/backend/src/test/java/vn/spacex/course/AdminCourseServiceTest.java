package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import vn.spacex.lesson.LessonRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private AdminCourseService service;

    private static AdminCourseRequest request(String slug) {
        return new AdminCourseRequest(slug, "Tin học 10", "Chương trình lớp 10", 1);
    }

    @Test
    void taoKhoaLuuDungCacTruongVaTraVeKhoaVuaTao() {
        when(courseRepository.existsBySlug("tin-hoc-10")).thenReturn(false);
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminCourseResponse response = service.create(request("tin-hoc-10"));

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getSlug()).isEqualTo("tin-hoc-10");
        assertThat(saved.getValue().getTitle()).isEqualTo("Tin học 10");
        assertThat(saved.getValue().getDescription()).isEqualTo("Chương trình lớp 10");
        assertThat(saved.getValue().getSortOrder()).isEqualTo(1);
        assertThat(response.slug()).isEqualTo("tin-hoc-10");
        assertThat(response.title()).isEqualTo("Tin học 10");
        assertThat(response.sortOrder()).isEqualTo(1);
    }

    @Test
    void taoKhoaTrungSlugNemLoiVaKhongGhiGi() {
        when(courseRepository.existsBySlug("tin-hoc-10")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("tin-hoc-10")))
                .isInstanceOf(CourseSlugAlreadyExistsException.class);

        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void haiNguoiTaoCungSlugCungLucThiNguoiThuaBiBaoTrungSlug() {
        when(courseRepository.existsBySlug("tin-hoc-10")).thenReturn(false);
        when(courseRepository.saveAndFlush(any(Course.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.create(request("tin-hoc-10")))
                .isInstanceOf(CourseSlugAlreadyExistsException.class);
    }

    @Test
    void suaKhoaDoiDungCacTruong() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(courseRepository.existsBySlugAndIdNot("tin-hoc-10-moi", 5L)).thenReturn(false);
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminCourseResponse response = service.update(5L,
                new AdminCourseRequest("tin-hoc-10-moi", "Tin học 10 mới", "Mô tả mới", 3));

        assertThat(khoa.getSlug()).isEqualTo("tin-hoc-10-moi");
        assertThat(khoa.getTitle()).isEqualTo("Tin học 10 mới");
        assertThat(khoa.getDescription()).isEqualTo("Mô tả mới");
        assertThat(khoa.getSortOrder()).isEqualTo(3);
        assertThat(response.slug()).isEqualTo("tin-hoc-10-moi");
        assertThat(response.sortOrder()).isEqualTo(3);
    }

    @Test
    void suaKhoaKhongTonTaiNemCourseNotFoundVaKhongKiemTraSlug() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("tin-hoc-10")))
                .isInstanceOf(CourseNotFoundException.class);

        verify(courseRepository, never()).existsBySlugAndIdNot(any(), any());
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void suaKhoaDoiSlugSangSlugCuaKhoaKhacNemLoiVaKhongDoiGi() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(courseRepository.existsBySlugAndIdNot("tin-hoc-11", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("tin-hoc-11")))
                .isInstanceOf(CourseSlugAlreadyExistsException.class);

        assertThat(khoa.getSlug()).isEqualTo("tin-hoc-10");
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void suaKhoaGiuNguyenSlugCuaChinhNoKhongBiXemLaTrung() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        // existsBySlugAndIdNot loại chính khóa này ra nên slug cũ của nó không bị tính là trùng.
        when(courseRepository.existsBySlugAndIdNot("tin-hoc-10", 5L)).thenReturn(false);
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminCourseResponse response = service.update(5L, request("tin-hoc-10"));

        assertThat(response.slug()).isEqualTo("tin-hoc-10");
    }

    @Test
    void xoaKhoaKhongConBaiThiXoaThat() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(lessonRepository.existsByCourseId(5L)).thenReturn(false);

        service.delete(5L);

        verify(courseRepository).delete(khoa);
        verify(courseRepository).flush();
    }

    @Test
    void xoaKhoaKhongTonTaiNemCourseNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(CourseNotFoundException.class);

        verify(courseRepository, never()).delete(any());
    }

    @Test
    void xoaKhoaConBaiNemLoiVaKhongXoa() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(lessonRepository.existsByCourseId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(CourseHasLessonsException.class);

        verify(courseRepository, never()).delete(any());
    }

    @Test
    void biThemBaiVaoKhoaDungLucXoaThiKhoaNgoaiChanVaBaoConBai() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(lessonRepository.existsByCourseId(5L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("fk")).when(courseRepository).flush();

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(CourseHasLessonsException.class);
    }
}
