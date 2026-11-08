package vn.spacex.lesson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import vn.spacex.common.api.PageResponse;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.course.CourseRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLessonServiceTest {

    private static final long ADMIN_ID = 1L;

    @Mock
    private AdminLessonRepository adminLessonRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AdminLessonService service;

    private static AdminLessonRequest request(String slug) {
        return new AdminLessonRequest(2L, slug, "Máy tính là gì", "Tóm tắt", "# Nội dung", 3);
    }

    private static AdminLesson baiNhap() {
        return AdminLesson.create(2L, ADMIN_ID, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt", "# Nội dung", 3);
    }

    // ---- Danh sách ----

    @Test
    void danhSachTruyenDungBoLocVaTrangVaSapTheoKhoaThuTuRoiId() {
        Lesson bai = mock(Lesson.class);
        when(bai.getId()).thenReturn(11L);
        when(bai.getCourseId()).thenReturn(2L);
        when(bai.getSlug()).thenReturn("bai-1");
        when(bai.getTitle()).thenReturn("Bài 1");
        when(bai.getSummary()).thenReturn(null);
        when(bai.getStatus()).thenReturn(LessonStatus.DRAFT);
        when(bai.getSortOrder()).thenReturn(4);
        Page<Lesson> trang = new PageImpl<>(List.of(bai), PageRequest.of(1, 10), 11);
        when(lessonRepository.findForAdmin(eq(2L), eq(LessonStatus.DRAFT), any(Pageable.class))).thenReturn(trang);

        PageResponse<AdminLessonSummaryResponse> response = service.list(2L, LessonStatus.DRAFT, 1, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(lessonRepository).findForAdmin(eq(2L), eq(LessonStatus.DRAFT), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("courseId", "sortOrder", "id");
        assertThat(pageable.getValue().getSort().stream().allMatch(Sort.Order::isAscending)).isTrue();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalItems()).isEqualTo(11);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).slug()).isEqualTo("bai-1");
        assertThat(response.items().get(0).status()).isEqualTo(LessonStatus.DRAFT);
    }

    @Test
    void danhSachKhongLocThiTruyenNullChoCaHaiBoLoc() {
        when(lessonRepository.findForAdmin(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<AdminLessonSummaryResponse> response = service.list(null, null, 0, 20);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalItems()).isZero();
    }

    // ---- Chi tiết ----

    @Test
    void chiTietTraVeCaBaiNhapKemNoiDung() {
        AdminLesson bai = baiNhap();
        when(adminLessonRepository.findById(11L)).thenReturn(Optional.of(bai));

        AdminLessonResponse response = service.get(11L);

        assertThat(response.status()).isEqualTo(LessonStatus.DRAFT);
        assertThat(response.contentMd()).isEqualTo("# Nội dung");
    }

    @Test
    void chiTietBaiKhongTonTaiNemLessonNotFound() {
        when(adminLessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(LessonNotFoundException.class);
    }

    // ---- Tạo ----

    @Test
    void taoBaiMacDinhLaNhapVaNguoiTaoLaAdminTuSession() {
        when(courseRepository.existsById(2L)).thenReturn(true);
        when(adminLessonRepository.existsBySlug("may-tinh-la-gi")).thenReturn(false);
        when(adminLessonRepository.saveAndFlush(any(AdminLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminLessonResponse response = service.create(ADMIN_ID, request("may-tinh-la-gi"));

        ArgumentCaptor<AdminLesson> saved = ArgumentCaptor.forClass(AdminLesson.class);
        verify(adminLessonRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(LessonStatus.DRAFT);
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(ADMIN_ID);
        assertThat(saved.getValue().getCourseId()).isEqualTo(2L);
        assertThat(saved.getValue().getSlug()).isEqualTo("may-tinh-la-gi");
        assertThat(saved.getValue().getContentMd()).isEqualTo("# Nội dung");
        assertThat(saved.getValue().getSortOrder()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(LessonStatus.DRAFT);
    }

    @Test
    void taoBaiVaoKhoaKhongTonTaiNemCourseNotFoundVaKhongGhiGi() {
        when(courseRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(ADMIN_ID, request("may-tinh-la-gi")))
                .isInstanceOf(CourseNotFoundException.class);

        verify(adminLessonRepository, never()).saveAndFlush(any());
    }

    @Test
    void taoBaiTrungSlugNemLoiVaKhongGhiGi() {
        when(courseRepository.existsById(2L)).thenReturn(true);
        when(adminLessonRepository.existsBySlug("may-tinh-la-gi")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ADMIN_ID, request("may-tinh-la-gi")))
                .isInstanceOf(LessonSlugAlreadyExistsException.class);

        verify(adminLessonRepository, never()).saveAndFlush(any());
    }

    @Test
    void haiNguoiTaoCungSlugCungLucThiNguoiThuaBiBaoTrungSlug() {
        when(courseRepository.existsById(2L)).thenReturn(true);
        when(adminLessonRepository.existsBySlug("may-tinh-la-gi")).thenReturn(false);
        when(adminLessonRepository.saveAndFlush(any(AdminLesson.class)))
                .thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.create(ADMIN_ID, request("may-tinh-la-gi")))
                .isInstanceOf(LessonSlugAlreadyExistsException.class);
    }

    // ---- Sửa ----

    @Test
    void suaBaiDoiCacTruongNhungGiuNguyenTrangThaiVaNguoiTao() {
        AdminLesson bai = baiNhap();
        bai.changeStatus(LessonStatus.PUBLISHED);
        when(adminLessonRepository.findById(11L)).thenReturn(Optional.of(bai));
        when(courseRepository.existsById(5L)).thenReturn(true);
        when(adminLessonRepository.existsBySlugAndIdNot("slug-moi", 11L)).thenReturn(false);
        when(adminLessonRepository.saveAndFlush(any(AdminLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminLessonResponse response = service.update(11L,
                new AdminLessonRequest(5L, "slug-moi", "Tiêu đề mới", null, "Nội dung mới", 9));

        assertThat(bai.getCourseId()).isEqualTo(5L);
        assertThat(bai.getSlug()).isEqualTo("slug-moi");
        assertThat(bai.getTitle()).isEqualTo("Tiêu đề mới");
        assertThat(bai.getSummary()).isNull();
        assertThat(bai.getContentMd()).isEqualTo("Nội dung mới");
        assertThat(bai.getSortOrder()).isEqualTo(9);
        assertThat(bai.getStatus()).isEqualTo(LessonStatus.PUBLISHED);
        assertThat(bai.getCreatedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.status()).isEqualTo(LessonStatus.PUBLISHED);
    }

    @Test
    void suaBaiKhongTonTaiNemLessonNotFoundVaKhongKiemTraGiThem() {
        when(adminLessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("a")))
                .isInstanceOf(LessonNotFoundException.class);

        verify(courseRepository, never()).existsById(any());
        verify(adminLessonRepository, never()).saveAndFlush(any());
    }

    @Test
    void suaBaiChuyenSangKhoaKhongTonTaiNemCourseNotFoundVaBaiGiuNguyen() {
        AdminLesson bai = baiNhap();
        when(adminLessonRepository.findById(11L)).thenReturn(Optional.of(bai));
        when(courseRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(11L, request("a")))
                .isInstanceOf(CourseNotFoundException.class);

        assertThat(bai.getSlug()).isEqualTo("may-tinh-la-gi");
        verify(adminLessonRepository, never()).saveAndFlush(any());
    }

    @Test
    void suaBaiDoiSangSlugCuaBaiKhacNemLoiVaKhongDoiGi() {
        AdminLesson bai = baiNhap();
        when(adminLessonRepository.findById(11L)).thenReturn(Optional.of(bai));
        when(courseRepository.existsById(2L)).thenReturn(true);
        when(adminLessonRepository.existsBySlugAndIdNot("bai-khac", 11L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(11L, request("bai-khac")))
                .isInstanceOf(LessonSlugAlreadyExistsException.class);

        assertThat(bai.getSlug()).isEqualTo("may-tinh-la-gi");
        verify(adminLessonRepository, never()).saveAndFlush(any());
    }

    // ---- Đổi trạng thái ----

    @Test
    void dangBaiDoiTrangThaiSangPublished() {
        AdminLesson bai = baiNhap();
        when(adminLessonRepository.findById(11L)).thenReturn(Optional.of(bai));
        when(adminLessonRepository.saveAndFlush(any(AdminLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminLessonResponse response = service.changeStatus(11L, LessonStatus.PUBLISHED);

        assertThat(bai.getStatus()).isEqualTo(LessonStatus.PUBLISHED);
        assertThat(response.status()).isEqualTo(LessonStatus.PUBLISHED);
    }

    @Test
    void anBaiDoiTrangThaiVeDraft() {
        AdminLesson bai = baiNhap();
        bai.changeStatus(LessonStatus.PUBLISHED);
        when(adminLessonRepository.findById(11L)).thenReturn(Optional.of(bai));
        when(adminLessonRepository.saveAndFlush(any(AdminLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(11L, LessonStatus.DRAFT);

        assertThat(bai.getStatus()).isEqualTo(LessonStatus.DRAFT);
    }

    @Test
    void doiTrangThaiBaiKhongTonTaiNemLessonNotFound() {
        when(adminLessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(99L, LessonStatus.PUBLISHED))
                .isInstanceOf(LessonNotFoundException.class);
    }

    // ---- Xóa ----

    @Test
    void xoaBaiGoiXoaDungBai() {
        AdminLesson bai = baiNhap();
        when(adminLessonRepository.findById(11L)).thenReturn(Optional.of(bai));

        service.delete(11L);

        verify(adminLessonRepository).delete(bai);
    }

    @Test
    void xoaBaiKhongTonTaiNemLessonNotFound() {
        when(adminLessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(LessonNotFoundException.class);

        verify(adminLessonRepository, never()).delete(any());
    }
}
