package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.lesson.LessonSummaryResponse;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void chuaDangNhapVanXemDuocDanhSachKhoaHoc() throws Exception {
        CategoryResponse nhom = new CategoryResponse(3L, "thieu-nien", "Thiếu niên");
        when(courseService.listCourses(any())).thenReturn(List.of(
                new CourseResponse(1L, "tin-hoc-10", "Tin học 10", "Chương trình lớp 10",
                        "/api/courses/tin-hoc-10/cover?v=1", 4, nhom, CourseLevel.BASIC, 40, 1_500_000L),
                new CourseResponse(2L, "tin-hoc-11", "Tin học 11", null, null, 0, nhom, CourseLevel.EXPERT, 0, 0L)));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$[0].title").value("Tin học 10"))
                .andExpect(jsonPath("$[0].description").value("Chương trình lớp 10"))
                .andExpect(jsonPath("$[0].coverImageUrl").value("/api/courses/tin-hoc-10/cover?v=1"))
                // Khóa chưa có ảnh bìa: trường vẫn có mặt và là null, để giao diện biết dùng ảnh mặc định.
                .andExpect(jsonPath("$[1].coverImageUrl").value(org.hamcrest.Matchers.nullValue()))
                // Số bài đã đăng của khóa; khóa chưa có bài vẫn trả 0 chứ không bỏ trường.
                .andExpect(jsonPath("$[0].lessonCount").value(4))
                .andExpect(jsonPath("$[1].lessonCount").value(0))
                .andExpect(jsonPath("$[0].category.slug").value("thieu-nien"))
                .andExpect(jsonPath("$[0].category.name").value("Thiếu niên"))
                .andExpect(jsonPath("$[0].level").value("BASIC"))
                .andExpect(jsonPath("$[0].totalHours").value(40))
                .andExpect(jsonPath("$[0].tuitionVnd").value(1500000))
                .andExpect(jsonPath("$[0].*", hasSize(10)));
    }

    @Test
    void thamSoBoLocDuocChuyenThanhBoLocChoService() throws Exception {
        when(courseService.listCourses(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/courses?category=thieu-nien&level=ADVANCED&duration=20-40&price=-2000000"
                        + "&sort=price_desc"))
                .andExpect(status().isOk());

        verify(courseService).listCourses(CourseFilter.of("thieu-nien", CourseLevel.ADVANCED, "20-40", "-2000000",
                "price_desc"));
    }

    @Test
    void boLocSaiBi400VaServiceKhongDuocGoi() throws Exception {
        for (String query : new String[] {"level=KHAC", "duration=abc", "price=5-1", "sort=title"}) {
            mockMvc.perform(get("/api/courses?" + query))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }

        verify(courseService, never()).listCourses(any());
    }

    @Test
    void khongCoKhoaHocNaoTraVeMangRongChuKhongPhaiLoi() throws Exception {
        when(courseService.listCourses(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void chuaDangNhapVanXemDuocDanhSachBaiCuaKhoa() throws Exception {
        when(courseService.listPublishedLessons("tin-hoc-10")).thenReturn(List.of(
                new LessonSummaryResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Giới thiệu máy tính")));

        mockMvc.perform(get("/api/courses/tin-hoc-10/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].slug").value("may-tinh-la-gi"))
                .andExpect(jsonPath("$[0].title").value("Máy tính là gì"))
                .andExpect(jsonPath("$[0].summary").value("Giới thiệu máy tính"));
    }

    @Test
    void danhSachBaiChiCoBonTruongVaKhongBaoGioCoNoiDung() throws Exception {
        when(courseService.listPublishedLessons("tin-hoc-10")).thenReturn(List.of(
                new LessonSummaryResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt")));

        mockMvc.perform(get("/api/courses/tin-hoc-10/lessons"))
                .andExpect(jsonPath("$[0].*", hasSize(5)))
                .andExpect(jsonPath("$[0].contentMd").doesNotExist())
                .andExpect(jsonPath("$[0].content").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void khoaKhongTonTaiBi404DangProblemJsonVaThongBaoKhongLapLaiSlugNguoiDungGui() throws Exception {
        doThrow(new CourseNotFoundException()).when(courseService).listPublishedLessons("khong-co-khoa-nay");

        // detail là câu cố định. Trường instance (chuẩn RFC 9457) là đường dẫn của chính yêu cầu nên có slug, không kiểm.
        mockMvc.perform(get("/api/courses/khong-co-khoa-nay/lessons"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));
    }

    @Test
    void chiHaiDuongDanDocLaCongKhaiDuongDanConLaiVanCanDangNhap() throws Exception {
        mockMvc.perform(get("/api/courses/tin-hoc-10"))
                .andExpect(status().isUnauthorized());

        verify(courseService, never()).listPublishedLessons(any());
    }

    @Test
    void ghiVaoCoursesKhiChuaDangNhapBi401KhongPhaiDuocPhep() throws Exception {
        mockMvc.perform(post("/api/courses").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
