package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import vn.spacex.lesson.LessonRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private vn.spacex.image.CourseCoverService courseCoverService;

    @InjectMocks
    private AdminCourseService service;

    @Test
    void danhSachChoAdminGanDuongDanAnhBiaChoKhoaCoAnh() {
        Course coAnh = mock(Course.class);
        when(coAnh.getId()).thenReturn(5L);
        when(coAnh.getSlug()).thenReturn("tin-hoc-10");
        Course chuaCoAnh = mock(Course.class);
        when(chuaCoAnh.getId()).thenReturn(6L);
        when(chuaCoAnh.getSlug()).thenReturn("tin-hoc-11");
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(coAnh, chuaCoAnh));
        when(courseCoverService.urlsByCourseId(java.util.Map.of(5L, "tin-hoc-10", 6L, "tin-hoc-11")))
                .thenReturn(java.util.Map.of(5L, "/api/courses/tin-hoc-10/cover?v=1"));

        List<AdminCourseResponse> ketQua = service.findAll();

        assertThat(ketQua.get(0).coverImageUrl()).isEqualTo("/api/courses/tin-hoc-10/cover?v=1");
        assertThat(ketQua.get(1).coverImageUrl()).isNull();
    }

    @Test
    void suaKhoaTraVeDuongDanAnhBiaHienCoCuaKhoaDo() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(courseRepository.existsBySlugAndIdNot("tin-hoc-10", 5L)).thenReturn(false);
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseCoverService.urlOfCourse(5L, "tin-hoc-10")).thenReturn("/api/courses/tin-hoc-10/cover?v=2");

        AdminCourseResponse response = service.update(5L, request("tin-hoc-10"));

        assertThat(response.coverImageUrl()).isEqualTo("/api/courses/tin-hoc-10/cover?v=2");
    }

    private static AdminCourseRequest request(String slug) {
        return new AdminCourseRequest(slug, "Tin học 10", "Chương trình lớp 10", 1, null, null);
    }

    private static AdminCourseRequest banKhoa(CourseStatus status, Long tuitionVnd) {
        return new AdminCourseRequest("tin-hoc-10", "Tin học 10", null, 1, status, tuitionVnd);
    }

    @Test
    void khoaMoiKhongGuiTrangThaiVaHocPhiThiLaBanNhapKhongDong() {
        coNhomMacDinh();
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminCourseResponse response = service.create(banKhoa(null, null));

        assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
        assertThat(response.tuitionVnd()).isZero();
    }

    @Test
    void taoKhoaCoTheDangNgayKemHocPhi() {
        coNhomMacDinh();
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminCourseResponse response = service.create(banKhoa(CourseStatus.PUBLISHED, 1_500_000L));

        assertThat(response.status()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(response.tuitionVnd()).isEqualTo(1_500_000L);
    }

    @Test
    void suaKhoaDoiTrangThaiVaHocPhi() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(5L, banKhoa(CourseStatus.ARCHIVED, 2_000_000L));

        assertThat(khoa.getStatus()).isEqualTo(CourseStatus.ARCHIVED);
        assertThat(khoa.getTuitionVnd()).isEqualTo(2_000_000L);
    }

    @Test
    void suaKhoaKhongGuiTrangThaiVaHocPhiThiGiuNguyen() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        khoa.changeStatus(CourseStatus.PUBLISHED);
        khoa.changeTuition(1_500_000L);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(5L, banKhoa(null, null));

        assertThat(khoa.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(khoa.getTuitionVnd()).isEqualTo(1_500_000L);
    }

    private void coNhomMacDinh() {
        Category nhom = mock(Category.class);
        when(nhom.getId()).thenReturn(3L);
        when(categoryRepository.findBySlug("khoa-cho-thieu-nien")).thenReturn(Optional.of(nhom));
    }

    @Test
    void khoaMoiVaoNhomMacDinhVaCoMaSinhTuSlug() {
        coNhomMacDinh();
        when(courseRepository.existsBySlug("tin-hoc-10")).thenReturn(false);
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request("tin-hoc-10"));

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCategoryId()).isEqualTo(3L);
        assertThat(saved.getValue().getCode()).isEqualTo("TINHOC10");
    }

    @Test
    void suaSlugKhongDoiMaVaNhomCuaKhoa() {
        Course khoa = Course.create("tin-hoc-10", "Tin học 10", null, 1);
        khoa.placeIn(3L, "TINHOC10");
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
        when(courseRepository.existsBySlugAndIdNot("ten-khac", 5L)).thenReturn(false);
        when(courseRepository.saveAndFlush(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(5L, request("ten-khac"));

        assertThat(khoa.getCategoryId()).isEqualTo(3L);
        assertThat(khoa.getCode()).isEqualTo("TINHOC10");
    }

    @Test
    void taoKhoaLuuDungCacTruongVaTraVeKhoaVuaTao() {
        coNhomMacDinh();
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
        coNhomMacDinh();
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
                new AdminCourseRequest("tin-hoc-10-moi", "Tin học 10 mới", "Mô tả mới", 3, null, null));

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

    @Test
    void danhSachKhoaTraVeDungThuTuCuaRepositoryKemSortOrder() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                Course.create("tin-hoc-10", "Tin học 10", "Lớp 10", 1),
                Course.create("tin-hoc-11", "Tin học 11", null, 2)));

        List<AdminCourseResponse> ketQua = service.findAll();

        assertThat(ketQua).extracting(AdminCourseResponse::slug).containsExactly("tin-hoc-10", "tin-hoc-11");
        assertThat(ketQua).extracting(AdminCourseResponse::sortOrder).containsExactly(1, 2);
        assertThat(ketQua.get(1).description()).isNull();
    }

    @Test
    void chuaCoKhoaNaoThiDanhSachRong() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }
}
