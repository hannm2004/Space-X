package vn.spacex.image;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.lesson.LessonNotFoundException;
import vn.spacex.user.Role;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminImageController.class, ImageController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ImageControllerTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageService service;

    private static AppUserDetails admin() {
        return new AppUserDetails(1L, "admin@example.com", null, "Quản trị viên", Role.ADMIN, true);
    }

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, "hs@example.com", null, "Học sinh", Role.LEARNER, true);
    }

    private static MockMultipartFile tep() {
        return new MockMultipartFile("file", "so-do.png", "image/png", PNG);
    }

    // ---- Tải lên ----

    @Test
    void khachHocSinhVaAdminThieuCsrfKhongTaiAnhLenDuoc() throws Exception {
        mockMvc.perform(multipart("/api/admin/lessons/11/images").file(tep()).with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/admin/lessons/11/images").file(tep()).with(user(hocSinh())).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(multipart("/api/admin/lessons/11/images").file(tep()).with(user(admin())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminTaiAnhThanhCongTra201VoiIdVaUrlDeChenVaoMarkdown() throws Exception {
        when(service.upload(eq(11L), eq("so-do.png"), any())).thenReturn(new ImageUploadResponse(7L, "/api/images/7"));

        mockMvc.perform(multipart("/api/admin/lessons/11/images").file(tep()).with(user(admin())).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.url").value("/api/images/7"));

        verify(service).upload(11L, "so-do.png", PNG);
    }

    @Test
    void thieuPhanFileBi400() throws Exception {
        mockMvc.perform(multipart("/api/admin/lessons/11/images").with(user(admin())).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void phanFileTenKhacBi400() throws Exception {
        MockMultipartFile saiTen = new MockMultipartFile("anh", "a.png", "image/png", PNG);

        mockMvc.perform(multipart("/api/admin/lessons/11/images").file(saiTen).with(user(admin())).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void taiAnhKhongPhaiAnhBi415DangProblemJson() throws Exception {
        when(service.upload(anyLong(), any(), any())).thenThrow(new UnsupportedImageTypeException());

        mockMvc.perform(multipart("/api/admin/lessons/11/images").file(tep()).with(user(admin())).with(csrf()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Chỉ nhận ảnh PNG, JPEG hoặc WebP hợp lệ"));
    }

    @Test
    void taiAnhQuaLonBi413() throws Exception {
        when(service.upload(anyLong(), any(), any())).thenThrow(new ImageTooLargeException());

        mockMvc.perform(multipart("/api/admin/lessons/11/images").file(tep()).with(user(admin())).with(csrf()))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.detail").value("Ảnh vượt quá 1 MB"));
    }

    @Test
    void taiAnhVaoBaiKhongTonTaiBi404() throws Exception {
        when(service.upload(anyLong(), any(), any())).thenThrow(new LessonNotFoundException());

        mockMvc.perform(multipart("/api/admin/lessons/99/images").file(tep()).with(user(admin())).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy bài học"));
    }

    // ---- Danh sách và xóa (Admin) ----

    @Test
    void danhSachAnhCuaBaiChiAdminXemDuocVaKhongCoDuLieuAnh() throws Exception {
        when(service.list(11L)).thenReturn(List.of(
                new ImageResponse(5L, "a.png", "image/png", 123, "/api/images/5")));

        mockMvc.perform(get("/api/admin/lessons/11/images")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/lessons/11/images").with(user(hocSinh()))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/lessons/11/images").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].*", hasSize(5)))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].fileName").value("a.png"))
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andExpect(jsonPath("$[0].sizeBytes").value(123))
                .andExpect(jsonPath("$[0].url").value("/api/images/5"))
                .andExpect(jsonPath("$[0].data").doesNotExist());
    }

    @Test
    void danhSachAnhCuaBaiKhongTonTaiBi404() throws Exception {
        when(service.list(99L)).thenThrow(new LessonNotFoundException());

        mockMvc.perform(get("/api/admin/lessons/99/images").with(user(admin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void xoaAnhChiAdminCoCsrfLamDuocTra204() throws Exception {
        mockMvc.perform(delete("/api/admin/images/5").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/images/5").with(user(hocSinh())).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/images/5").with(user(admin()))).andExpect(status().isForbidden());
        verifyNoInteractions(service);

        mockMvc.perform(delete("/api/admin/images/5").with(user(admin())).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).delete(5L);
    }

    @Test
    void xoaAnhKhongTonTaiBi404() throws Exception {
        doThrow(new ImageNotFoundException()).when(service).delete(99L);

        mockMvc.perform(delete("/api/admin/images/99").with(user(admin())).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy ảnh"));
    }

    // ---- Xem ảnh ----

    @Test
    void khachChuaDangNhapKhongXemDuocAnh() throws Exception {
        mockMvc.perform(get("/api/images/5")).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void hocSinhXemAnhNhanDungLoaiAnhVaDuLieuVaServiceDuocHoiVoiQuyenHocSinh() throws Exception {
        when(service.load(5L, false)).thenReturn(new StoredImage("image/png", PNG));

        mockMvc.perform(get("/api/images/5").with(user(hocSinh())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("private")));

        verify(service).load(5L, false);
    }

    @Test
    void adminXemAnhDuocHoiVoiQuyenAdminDeThayCaAnhCuaBaiNhap() throws Exception {
        when(service.load(5L, true)).thenReturn(new StoredImage("image/jpeg", PNG));

        mockMvc.perform(get("/api/images/5").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));

        verify(service).load(5L, true);
        verify(service, never()).load(anyLong(), eq(false));
    }

    @Test
    void anhKhongTonTaiHoacThuocBaiNhapBi404ThongBaoCoDinh() throws Exception {
        when(service.load(eq(99L), anyBoolean())).thenThrow(new ImageNotFoundException());

        mockMvc.perform(get("/api/images/99").with(user(hocSinh())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy ảnh"));
    }

    @Test
    void idKhongPhaiSoBi400ThongBaoCoDinh() throws Exception {
        mockMvc.perform(get("/api/images/abc").with(user(hocSinh())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tham số trong đường dẫn không hợp lệ"));
        mockMvc.perform(delete("/api/admin/images/abc").with(user(admin())).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
