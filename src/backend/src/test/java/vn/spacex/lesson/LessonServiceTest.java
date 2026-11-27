package vn.spacex.lesson;

import static org.mockito.ArgumentMatchers.eq;
import vn.spacex.access.Reader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.spacex.common.api.PageResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonContentRepository lessonContentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private vn.spacex.image.CoverImageLookup coverImageLookup;

    @Mock
    private vn.spacex.course.CourseRepository courseRepository;

    @InjectMocks
    private LessonService lessonService;

    private static final Reader HOC_SINH = new Reader(7L, false);

    private static final ContentRef KHOA = new ContentRef("tin-hoc-10", "Tin học 10");

    private static Lesson baiTrongDanhSach(long id, String slug, String tieuDe) {
        // lenient: tùy vị trí bài đang đọc mà dịch vụ chỉ đọc một vài trường của các bài lân cận.
        Lesson bai = mock(Lesson.class);
        lenient().when(bai.getId()).thenReturn(id);
        lenient().when(bai.getSlug()).thenReturn(slug);
        lenient().when(bai.getTitle()).thenReturn(tieuDe);
        return bai;
    }

    private void dungKhoa5() {
        vn.spacex.course.Course khoa = mock(vn.spacex.course.Course.class);
        when(khoa.getSlug()).thenReturn("tin-hoc-10");
        when(khoa.getTitle()).thenReturn("Tin học 10");
        when(courseRepository.findById(5L)).thenReturn(Optional.of(khoa));
    }

    // Dựng bài đang đọc (mã 12, khóa 5) và danh sách các bài đã đăng cùng khóa theo đúng thứ tự trang khóa học.
    private void dungBaiThuocKhoa5(Lesson... baiCungKhoa) {
        LessonContent bai = mock(LessonContent.class);
        when(bai.getId()).thenReturn(12L);
        when(bai.getSlug()).thenReturn("bai-giua");
        when(bai.getTitle()).thenReturn("Bài giữa");
        when(bai.getContentMd()).thenReturn("x");
        when(bai.getCourseId()).thenReturn(5L);
        when(lessonContentRepository.findReadableBySlug("bai-giua", 7L, false))
                .thenReturn(Optional.of(bai));
        dungKhoa5();
        when(lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(5L, LessonStatus.PUBLISHED))
                .thenReturn(List.of(baiCungKhoa));
    }

    @Test
    void baiDuocDocTraVeDayDuTruongVaHoiDungQuyenCuaNguoiDoc() {
        LessonContent bai = mock(LessonContent.class);
        when(bai.getId()).thenReturn(11L);
        when(bai.getSlug()).thenReturn("may-tinh-la-gi");
        when(bai.getTitle()).thenReturn("Máy tính là gì");
        when(bai.getSummary()).thenReturn("Tóm tắt");
        when(bai.getContentMd()).thenReturn("# Nội dung");
        when(bai.getCourseId()).thenReturn(5L);
        when(lessonContentRepository.findReadableBySlug("may-tinh-la-gi", 7L, false))
                .thenReturn(Optional.of(bai));
        dungKhoa5();
        Lesson baiDangDoc = baiTrongDanhSach(11L, "may-tinh-la-gi", "Máy tính là gì");
        when(lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(5L, LessonStatus.PUBLISHED))
                .thenReturn(List.of(baiDangDoc));

        LessonDetailResponse ketQua = lessonService.getLesson("may-tinh-la-gi", HOC_SINH);

        assertThat(ketQua).isEqualTo(new LessonDetailResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt",
                "# Nội dung", null, KHOA, null, null));
        // Quyền đọc nằm ngay trong truy vấn, nên bài không được xem không bao giờ được nạp lên.
        verify(lessonContentRepository).findReadableBySlug("may-tinh-la-gi", 7L, false);
    }

    @Test
    void baiGiuaKhoaCoCaBaiTruocVaBaiSauTheoThuTuTrangKhoaHoc() {
        dungBaiThuocKhoa5(
                baiTrongDanhSach(11L, "bai-dau", "Bài đầu"),
                baiTrongDanhSach(12L, "bai-giua", "Bài giữa"),
                baiTrongDanhSach(13L, "bai-cuoi", "Bài cuối"));

        LessonDetailResponse ketQua = lessonService.getLesson("bai-giua", HOC_SINH);

        assertThat(ketQua.course()).isEqualTo(KHOA);
        assertThat(ketQua.previous()).isEqualTo(new ContentRef("bai-dau", "Bài đầu"));
        assertThat(ketQua.next()).isEqualTo(new ContentRef("bai-cuoi", "Bài cuối"));
    }

    @Test
    void baiDauKhoaKhongCoBaiTruoc() {
        dungBaiThuocKhoa5(
                baiTrongDanhSach(12L, "bai-giua", "Bài giữa"),
                baiTrongDanhSach(13L, "bai-cuoi", "Bài cuối"));

        LessonDetailResponse ketQua = lessonService.getLesson("bai-giua", HOC_SINH);

        assertThat(ketQua.previous()).isNull();
        assertThat(ketQua.next()).isEqualTo(new ContentRef("bai-cuoi", "Bài cuối"));
    }

    @Test
    void baiCuoiKhoaKhongCoBaiSau() {
        dungBaiThuocKhoa5(
                baiTrongDanhSach(11L, "bai-dau", "Bài đầu"),
                baiTrongDanhSach(12L, "bai-giua", "Bài giữa"));

        LessonDetailResponse ketQua = lessonService.getLesson("bai-giua", HOC_SINH);

        assertThat(ketQua.previous()).isEqualTo(new ContentRef("bai-dau", "Bài đầu"));
        assertThat(ketQua.next()).isNull();
    }

    // Danh sách chỉ lấy bài PUBLISHED của đúng khóa (xem truy vấn), nên bài nháp nằm giữa hai bài đã đăng không bao giờ
    // thành "bài sau", và bài của khóa khác không lọt vào.
    @Test
    void baiTruocVaBaiSauChiLayTuBaiDaDangCuaDungKhoaDo() {
        dungBaiThuocKhoa5(baiTrongDanhSach(12L, "bai-giua", "Bài giữa"));

        lessonService.getLesson("bai-giua", HOC_SINH);

        verify(lessonRepository).findByCourseIdAndStatusOrderBySortOrderAscIdAsc(5L, LessonStatus.PUBLISHED);
    }

    @Test
    void khongCoBaiDaDangVoiSlugNayNemLessonNotFound() {
        when(lessonContentRepository.findReadableBySlug("bai-nhap", 7L, false))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getLesson("bai-nhap", HOC_SINH))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void adminDocBaiThiTruyVanNhanCoAdmin() {
        when(lessonContentRepository.findReadableBySlug("bai-nhap", 1L, true)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getLesson("bai-nhap", new Reader(1L, true)))
                .isInstanceOf(LessonNotFoundException.class);

        verify(lessonContentRepository).findReadableBySlug("bai-nhap", 1L, true);
    }

    @Test
    void timKiemCatKhoangTrangVaTraVeBonTruongCungThongTinPhanTrang() {
        Lesson bai = mock(Lesson.class);
        when(bai.getId()).thenReturn(11L);
        when(bai.getSlug()).thenReturn("thong-tin-va-du-lieu");
        when(bai.getTitle()).thenReturn("Thông tin và dữ liệu");
        when(bai.getSummary()).thenReturn("Tóm tắt");
        Pageable trang = PageRequest.of(1, 5);
        when(lessonRepository.searchReadable("thông tin", 7L, false, trang)).thenReturn(new PageImpl<>(List.of(bai), trang, 6));

        PageResponse<LessonSummaryResponse> ketQua = lessonService.search("  thông tin  ", HOC_SINH, 1, 5);

        assertThat(ketQua).isEqualTo(new PageResponse<>(
                List.of(new LessonSummaryResponse(11L, "thong-tin-va-du-lieu", "Thông tin và dữ liệu", "Tóm tắt")),
                1, 5, 6));
    }

    // Thứ tự theo độ liên quan nằm trong câu truy vấn; nếu truyền thêm Sort thì Spring sẽ ghép vào sau và làm sai.
    @Test
    void timKiemKhongTruyenSapXepVaoTruyVan() {
        when(lessonRepository.searchReadable(anyString(), eq(7L), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        lessonService.search("thuật toán", HOC_SINH, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(lessonRepository).searchReadable(anyString(), eq(7L), eq(false), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void timKiemGanDuongDanAnhBiaChoBaiCoAnhBia() {
        Lesson co = mock(Lesson.class);
        when(co.getId()).thenReturn(11L);
        when(co.getSlug()).thenReturn("a");
        when(co.getTitle()).thenReturn("A");
        Lesson khong = mock(Lesson.class);
        when(khong.getId()).thenReturn(12L);
        when(khong.getSlug()).thenReturn("b");
        when(khong.getTitle()).thenReturn("B");
        Pageable trang = PageRequest.of(0, 20);
        when(lessonRepository.searchReadable("a", 7L, false, trang)).thenReturn(new PageImpl<>(List.of(co, khong), trang, 2));
        when(coverImageLookup.urlsByLessonId(List.of(11L, 12L))).thenReturn(java.util.Map.of(11L, "/api/images/5"));

        PageResponse<LessonSummaryResponse> ketQua = lessonService.search("a", HOC_SINH, 0, 20);

        assertThat(ketQua.items()).extracting(LessonSummaryResponse::coverImageUrl)
                .containsExactly("/api/images/5", null);
    }

    @Test
    void baiDaDangTraVeDuongDanAnhBiaNeuCo() {
        dungBaiThuocKhoa5(baiTrongDanhSach(12L, "bai-giua", "Bài giữa"));
        when(coverImageLookup.urlsByLessonId(List.of(12L))).thenReturn(java.util.Map.of(12L, "/api/images/5"));

        assertThat(lessonService.getLesson("bai-giua", HOC_SINH).coverImageUrl()).isEqualTo("/api/images/5");
    }
}
