package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.spacex.lesson.Lesson;
import vn.spacex.lesson.LessonCountByCourse;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Mock
    private vn.spacex.image.CourseCoverService courseCoverService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void danhSachKhoaGanDuongDanAnhBiaChoKhoaCoAnhVaNullChoKhoaChuaCo() {
        Course coAnh = mock(Course.class);
        when(coAnh.getId()).thenReturn(5L);
        when(coAnh.getSlug()).thenReturn("tin-hoc-10");
        Course chuaCoAnh = mock(Course.class);
        when(chuaCoAnh.getId()).thenReturn(6L);
        when(chuaCoAnh.getSlug()).thenReturn("tin-hoc-11");
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(coAnh, chuaCoAnh));
        when(courseCoverService.urlsByCourseId(java.util.Map.of(5L, "tin-hoc-10", 6L, "tin-hoc-11")))
                .thenReturn(java.util.Map.of(5L, "/api/courses/tin-hoc-10/cover?v=1"));

        List<CourseResponse> ketQua = courseService.listCourses();

        assertThat(ketQua).hasSize(2);
        assertThat(ketQua.get(0).coverImageUrl()).isEqualTo("/api/courses/tin-hoc-10/cover?v=1");
        assertThat(ketQua.get(1).coverImageUrl()).isNull();
    }

    private static LessonCountByCourse soBai(long maKhoa, LessonStatus trangThai, long tong) {
        LessonCountByCourse dong = mock(LessonCountByCourse.class);
        // lenient: dòng bài nháp bị bỏ qua nên dịch vụ không đọc mã khóa của nó.
        lenient().when(dong.getCourseId()).thenReturn(maKhoa);
        lenient().when(dong.getStatus()).thenReturn(trangThai);
        lenient().when(dong.getTotal()).thenReturn(tong);
        return dong;
    }

    // Học sinh chỉ thấy bài đã đăng, nên số bài trên thẻ khóa không được cộng bài nháp.
    @Test
    void danhSachKhoaKemSoBaiDaDangKhongTinhBaiNhapVaKhoaChuaCoBaiLaKhong() {
        Course coBai = mock(Course.class);
        when(coBai.getId()).thenReturn(5L);
        when(coBai.getSlug()).thenReturn("tin-hoc-10");
        Course chuaCoBai = mock(Course.class);
        when(chuaCoBai.getId()).thenReturn(6L);
        when(chuaCoBai.getSlug()).thenReturn("tin-hoc-11");
        when(courseRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(coBai, chuaCoBai));
        // Dựng mock ra biến trước: gọi soBai() ngay trong when(...) sẽ lồng hai lần khai báo giả lập vào nhau.
        LessonCountByCourse daDang = soBai(5L, LessonStatus.PUBLISHED, 4);
        LessonCountByCourse nhap = soBai(5L, LessonStatus.DRAFT, 3);
        when(lessonRepository.countLessonsByCourseAndStatus()).thenReturn(List.of(daDang, nhap));

        List<CourseResponse> ketQua = courseService.listCourses();

        assertThat(ketQua).extracting(CourseResponse::lessonCount).containsExactly(4, 0);
        // Một truy vấn đếm cho cả danh sách, không phải mỗi khóa một truy vấn.
        verify(lessonRepository, times(1)).countLessonsByCourseAndStatus();
    }

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
