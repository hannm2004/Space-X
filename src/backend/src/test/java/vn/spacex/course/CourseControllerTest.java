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
        when(courseService.listCourses()).thenReturn(List.of(
                new CourseResponse(1L, "tin-hoc-10", "Tin học 10", "Chương trình lớp 10"),
                new CourseResponse(2L, "tin-hoc-11", "Tin học 11", null)));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$[0].title").value("Tin học 10"))
                .andExpect(jsonPath("$[0].description").value("Chương trình lớp 10"));
    }

    @Test
    void khongCoKhoaHocNaoTraVeMangRongChuKhongPhaiLoi() throws Exception {
        when(courseService.listCourses()).thenReturn(List.of());

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
                .andExpect(jsonPath("$[0].*", hasSize(4)))
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
