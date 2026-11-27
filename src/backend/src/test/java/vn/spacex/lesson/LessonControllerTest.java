package vn.spacex.lesson;

import vn.spacex.access.Reader;
import vn.spacex.user.Role;
import vn.spacex.auth.AppUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.common.api.PageResponse;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final Reader HOC_SINH = new Reader(7L, false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LessonService lessonService;

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, "hs@example.com", null, "Học sinh", Role.LEARNER, true);
    }

    @Test
    void chuaDangNhapDocNoiDungBaiBi401VaKhongGoiService() throws Exception {
        mockMvc.perform(get("/api/lessons/may-tinh-la-gi"))
                .andExpect(status().isUnauthorized());

        verify(lessonService, never()).getLesson(any(), any());
    }

    @Test
    void daDangNhapDocBaiDaDangThanhCongKemNoiDungMarkdown() throws Exception {
        when(lessonService.getLesson("may-tinh-la-gi", HOC_SINH)).thenReturn(
                new LessonDetailResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Giới thiệu máy tính",
                        "# Máy tính\n\nNội dung **đậm**"));

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user(hocSinh())))
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
        when(lessonService.getLesson("may-tinh-la-gi", HOC_SINH)).thenReturn(
                new LessonDetailResponse(11L, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt", "Nội dung"));

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user(hocSinh())))
                .andExpect(jsonPath("$.*", hasSize(9)))
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.courseId").doesNotExist())
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void phanHoiCoKhoaVaBaiTruocBaiSauChiGomSlugVaTieuDe() throws Exception {
        when(lessonService.getLesson("bai-giua", HOC_SINH)).thenReturn(new LessonDetailResponse(12L, "bai-giua",
                "Bài giữa", null, "Nội dung", null, new ContentRef("tin-hoc-10", "Tin học 10"),
                new ContentRef("bai-dau", "Bài đầu"), null));

        mockMvc.perform(get("/api/lessons/bai-giua").with(user(hocSinh())))
                .andExpect(jsonPath("$.course.slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$.course.title").value("Tin học 10"))
                .andExpect(jsonPath("$.course.*", hasSize(2)))
                .andExpect(jsonPath("$.previous.slug").value("bai-dau"))
                .andExpect(jsonPath("$.previous.title").value("Bài đầu"))
                .andExpect(jsonPath("$.previous.*", hasSize(2)))
                // Bài cuối khóa: trường next vẫn có mặt và là null để giao diện biết không hiện nút "Bài sau".
                .andExpect(jsonPath("$.next").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void baiKhongTonTaiHoacLaBaiNhapBi404DangProblemJsonVaThongBaoKhongLapLaiSlug() throws Exception {
        doThrow(new LessonNotFoundException()).when(lessonService).getLesson("bai-nhap", HOC_SINH);

        // detail là câu cố định. Trường instance (chuẩn RFC 9457) là đường dẫn của chính yêu cầu nên có slug, không kiểm.
        mockMvc.perform(get("/api/lessons/bai-nhap").with(user(hocSinh())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy bài học"));
    }

    @Test
    void quyenCuaNguoiDocLayTuSessionAdminDuocHoiVoiCoAdmin() throws Exception {
        AppUserDetails admin = new AppUserDetails(1L, "admin@example.com", null, "Quản trị", Role.ADMIN, true);

        mockMvc.perform(get("/api/lessons/bai-nhap").param("userId", "999").with(user(admin)));

        verify(lessonService).getLesson("bai-nhap", new Reader(1L, true));
    }

    @Test
    void chuaDangNhapTimKiemBi401VaKhongGoiService() throws Exception {
        mockMvc.perform(get("/api/lessons").param("q", "thông tin"))
                .andExpect(status().isUnauthorized());

        verify(lessonService, never()).search(anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void timKiemTraVeDungHinhDangPhanTrangVaMoiBaiChiCoBonTruong() throws Exception {
        when(lessonService.search("thông tin", HOC_SINH, 0, 20)).thenReturn(new PageResponse<>(
                List.of(new LessonSummaryResponse(11L, "thong-tin-va-du-lieu", "Thông tin và dữ liệu", "Tóm tắt")),
                0, 20, 1));

        mockMvc.perform(get("/api/lessons").param("q", "thông tin").with(user(hocSinh())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].*", hasSize(5)))
                .andExpect(jsonPath("$.items[0].slug").value("thong-tin-va-du-lieu"))
                .andExpect(jsonPath("$.items[0].contentMd").doesNotExist())
                .andExpect(jsonPath("$.items[0].status").doesNotExist());
    }

    @Test
    void timKiemChuyenTrangVaKichThuocTrangVaoService() throws Exception {
        when(lessonService.search("thuật toán", HOC_SINH, 2, 5)).thenReturn(new PageResponse<>(List.of(), 2, 5, 0));

        mockMvc.perform(get("/api/lessons").param("q", "thuật toán").param("page", "2").param("size", "5")
                        .with(user(hocSinh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items", hasSize(0)));

        verify(lessonService).search("thuật toán", HOC_SINH, 2, 5);
    }

    @Test
    void thieuTuKhoaHoacTuKhoaRongBi400VaKhongGoiService() throws Exception {
        mockMvc.perform(get("/api/lessons").with(user(hocSinh()))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lessons").param("q", "").with(user(hocSinh()))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lessons").param("q", "   ").with(user(hocSinh())))
                .andExpect(status().isBadRequest());

        verify(lessonService, never()).search(anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void tuKhoaDai200KyTuQuaDuocNhungDai201KyTuBi400() throws Exception {
        when(lessonService.search(eq("a".repeat(200)), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/lessons").param("q", "a".repeat(200)).with(user(hocSinh())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lessons").param("q", "a".repeat(201)).with(user(hocSinh())))
                .andExpect(status().isBadRequest());

        verify(lessonService, never()).search(eq("a".repeat(201)), any(), anyInt(), anyInt());
    }

    @Test
    void kichThuocTrangNgoaiKhoang1Den50HoacTrangAmBi400() throws Exception {
        when(lessonService.search(anyString(), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 50, 0));

        mockMvc.perform(get("/api/lessons").param("q", "tin").param("size", "50").with(user(hocSinh())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lessons").param("q", "tin").param("size", "51").with(user(hocSinh())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lessons").param("q", "tin").param("size", "0").with(user(hocSinh())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lessons").param("q", "tin").param("page", "-1").with(user(hocSinh())))
                .andExpect(status().isBadRequest());

        verify(lessonService, never()).search(anyString(), any(), eq(0), eq(51));
        verify(lessonService, never()).search(anyString(), any(), eq(0), eq(0));
        verify(lessonService, never()).search(anyString(), any(), eq(-1), anyInt());
    }
}
