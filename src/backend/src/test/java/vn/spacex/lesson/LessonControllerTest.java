package vn.spacex.lesson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LessonService lessonService;

    @Test
    void chuaDangNhapDocNoiDungBaiBi401VaKhongGoiService() throws Exception {
        mockMvc.perform(get("/api/lessons/may-tinh-la-gi"))
                .andExpect(status().isUnauthorized());

        verify(lessonService, never()).getPublishedLesson(any());
    }

    @Test
    void daDangNhapDocBaiDaDangThanhCongKemNoiDungMarkdown() throws Exception {
        when(lessonService.getPublishedLesson("may-tinh-la-gi")).thenReturn(
                new LessonDetailResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Giới thiệu máy tính",
                        "# Máy tính\n\nNội dung **đậm**"));

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user("hocsinh")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.slug").value("may-tinh-la-gi"))
                .andExpect(jsonPath("$.title").value("Máy tính là gì"))
                .andExpect(jsonPath("$.summary").value("Giới thiệu máy tính"))
                .andExpect(jsonPath("$.contentMd").value("# Máy tính\n\nNội dung **đậm**"));
    }

    @Test
    void phanHoiChiCoNamTruongVaKhongLoTrangThaiHayMaKhoa() throws Exception {
        when(lessonService.getPublishedLesson("may-tinh-la-gi")).thenReturn(
                new LessonDetailResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt", "Nội dung"));

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user("hocsinh")))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.courseId").doesNotExist())
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void baiKhongTonTaiHoacLaBaiNhapBi404DangProblemJsonVaThongBaoKhongLapLaiSlug() throws Exception {
        doThrow(new LessonNotFoundException()).when(lessonService).getPublishedLesson("bai-nhap");

        // detail là câu cố định. Trường instance (chuẩn RFC 9457) là đường dẫn của chính yêu cầu nên có slug, không kiểm.
        mockMvc.perform(get("/api/lessons/bai-nhap").with(user("hocsinh")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy bài học"));
    }
}
