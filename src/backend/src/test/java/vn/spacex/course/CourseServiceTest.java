package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
