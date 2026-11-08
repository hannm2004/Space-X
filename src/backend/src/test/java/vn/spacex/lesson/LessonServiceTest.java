package vn.spacex.lesson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonContentRepository lessonContentRepository;

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
}
