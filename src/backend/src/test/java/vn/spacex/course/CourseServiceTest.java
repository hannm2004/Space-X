package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.spacex.lesson.Lesson;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private vn.spacex.image.CoverImageLookup coverImageLookup;

    @InjectMocks
    private CourseService courseService;

    @Test
    void danhSachKhoaLayTheoThuTuSortOrderRoiId() {
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        assertThat(courseService.listCourses()).isEmpty();

        verify(courseRepository).findAllByOrderBySortOrderAscIdAsc();
    }

    @Test
    void khoaKhongTonTaiNemCourseNotFoundVaKhongTruyVanBaiNao() {
        when(courseRepository.findBySlug("khong-co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.listPublishedLessons("khong-co"))
                .isInstanceOf(CourseNotFoundException.class);

        verifyNoInteractions(lessonRepository);
    }

    @Test
    void khoaTonTaiChiHoiBaiDaDangCuaDungKhoaDo() {
        Course khoa = mock(Course.class);
        when(khoa.getId()).thenReturn(5L);
        when(courseRepository.findBySlug("tin-hoc-10")).thenReturn(Optional.of(khoa));
        when(lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(5L, LessonStatus.PUBLISHED))
                .thenReturn(List.of());

        assertThat(courseService.listPublishedLessons("tin-hoc-10")).isEmpty();

        verify(lessonRepository).findByCourseIdAndStatusOrderBySortOrderAscIdAsc(5L, LessonStatus.PUBLISHED);
    }

    @Test
    void danhSachBaiCuaKhoaGanDuongDanAnhBia() {
        Course khoa = mock(Course.class);
        when(khoa.getId()).thenReturn(5L);
        when(courseRepository.findBySlug("tin-hoc-10")).thenReturn(Optional.of(khoa));
        Lesson co = mock(Lesson.class);
        when(co.getId()).thenReturn(11L);
        Lesson khong = mock(Lesson.class);
        when(khong.getId()).thenReturn(12L);
        when(lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(5L, LessonStatus.PUBLISHED))
                .thenReturn(List.of(co, khong));
        when(coverImageLookup.urlsByLessonId(List.of(11L, 12L))).thenReturn(java.util.Map.of(12L, "/api/images/9"));

        assertThat(courseService.listPublishedLessons("tin-hoc-10"))
                .extracting(vn.spacex.lesson.LessonSummaryResponse::coverImageUrl)
                .containsExactly(null, "/api/images/9");
    }
}
