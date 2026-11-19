package vn.spacex.lesson;

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

    @InjectMocks
    private LessonService lessonService;

    @Test
    void baiDaDangTraVeDayDuNamTruongVaChiHoiBaiPublished() {
        LessonContent bai = mock(LessonContent.class);
        when(bai.getId()).thenReturn(11L);
        when(bai.getSlug()).thenReturn("may-tinh-la-gi");
        when(bai.getTitle()).thenReturn("Máy tính là gì");
        when(bai.getSummary()).thenReturn("Tóm tắt");
        when(bai.getContentMd()).thenReturn("# Nội dung");
        when(lessonContentRepository.findBySlugAndStatus("may-tinh-la-gi", LessonStatus.PUBLISHED))
                .thenReturn(Optional.of(bai));

        LessonDetailResponse ketQua = lessonService.getPublishedLesson("may-tinh-la-gi");

        assertThat(ketQua).isEqualTo(
                new LessonDetailResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt", "# Nội dung"));
        // Trạng thái PUBLISHED nằm ngay trong truy vấn, nên bài nháp không bao giờ được nạp lên.
        verify(lessonContentRepository).findBySlugAndStatus("may-tinh-la-gi", LessonStatus.PUBLISHED);
    }

    @Test
    void khongCoBaiDaDangVoiSlugNayNemLessonNotFound() {
        when(lessonContentRepository.findBySlugAndStatus("bai-nhap", LessonStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getPublishedLesson("bai-nhap"))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void timKiemCatKhoangTrangVaTraVeBonTruongCungThongTinPhanTrang() {
        Lesson bai = mock(Lesson.class);
        when(bai.getId()).thenReturn(11L);
        when(bai.getSlug()).thenReturn("thong-tin-va-du-lieu");
        when(bai.getTitle()).thenReturn("Thông tin và dữ liệu");
        when(bai.getSummary()).thenReturn("Tóm tắt");
        Pageable trang = PageRequest.of(1, 5);
        when(lessonRepository.searchPublished("thông tin", trang)).thenReturn(new PageImpl<>(List.of(bai), trang, 6));

        PageResponse<LessonSummaryResponse> ketQua = lessonService.search("  thông tin  ", 1, 5);

        assertThat(ketQua).isEqualTo(new PageResponse<>(
                List.of(new LessonSummaryResponse(11L, "thong-tin-va-du-lieu", "Thông tin và dữ liệu", "Tóm tắt")),
                1, 5, 6));
    }

    // Thứ tự theo độ liên quan nằm trong câu truy vấn; nếu truyền thêm Sort thì Spring sẽ ghép vào sau và làm sai.
    @Test
    void timKiemKhongTruyenSapXepVaoTruyVan() {
        when(lessonRepository.searchPublished(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        lessonService.search("thuật toán", 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(lessonRepository).searchPublished(anyString(), captor.capture());
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
        when(lessonRepository.searchPublished("a", trang)).thenReturn(new PageImpl<>(List.of(co, khong), trang, 2));
        when(coverImageLookup.urlsByLessonId(List.of(11L, 12L))).thenReturn(java.util.Map.of(11L, "/api/images/5"));

        PageResponse<LessonSummaryResponse> ketQua = lessonService.search("a", 0, 20);

        assertThat(ketQua.items()).extracting(LessonSummaryResponse::coverImageUrl)
                .containsExactly("/api/images/5", null);
    }

    @Test
    void baiDaDangTraVeDuongDanAnhBiaNeuCo() {
        LessonContent bai = mock(LessonContent.class);
        when(bai.getId()).thenReturn(11L);
        when(bai.getSlug()).thenReturn("a");
        when(bai.getTitle()).thenReturn("A");
        when(bai.getContentMd()).thenReturn("x");
        when(lessonContentRepository.findBySlugAndStatus("a", LessonStatus.PUBLISHED)).thenReturn(Optional.of(bai));
        when(coverImageLookup.urlsByLessonId(List.of(11L))).thenReturn(java.util.Map.of(11L, "/api/images/5"));

        assertThat(lessonService.getPublishedLesson("a").coverImageUrl()).isEqualTo("/api/images/5");
    }
}
