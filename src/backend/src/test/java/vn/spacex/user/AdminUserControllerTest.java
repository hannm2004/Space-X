package vn.spacex.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.auth.OtherSessionsRevoker;
import vn.spacex.common.api.PageResponse;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminUserControllerTest {

    private static final long ADMIN_ID = 1L;
    private static final String EMAIL_HS = "hs@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService service;

    @MockitoBean
    private OtherSessionsRevoker revoker;

    private static AppUserDetails admin() {
        return new AppUserDetails(ADMIN_ID, "admin@example.com", null, "Quản trị viên", Role.ADMIN, true);
    }

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, EMAIL_HS, null, "Học sinh", Role.LEARNER, true);
    }

    private static AdminUserResponse nguoiDung(boolean enabled) {
        return new AdminUserResponse(7L, EMAIL_HS, "Học sinh", Role.LEARNER, AuthProvider.LOCAL, enabled,
                LocalDateTime.of(2026, 11, 1, 8, 0));
    }

    // ---- Phân quyền và CSRF: luật /api/admin/** thật của SecurityConfig ----

    @Test
    void khachChuaDangNhapGoiBaEndpointDeuBi401VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/users/7/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/users/7/reset-password").with(csrf())).andExpect(status().isUnauthorized());

        verifyNoInteractions(service, revoker);
    }

    @Test
    void hocSinhGoiBaEndpointDeuBi403VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user(hocSinh())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(patch("/api/admin/users/7/status").with(user(hocSinh())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/7/reset-password").with(user(hocSinh())).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service, revoker);
    }

    @Test
    void adminThieuCsrfTokenKhiGhiThiBi403() throws Exception {
        mockMvc.perform(patch("/api/admin/users/7/status").with(user(admin()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/7/reset-password").with(user(admin())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service, revoker);
    }

    // ---- Danh sách ----

    @Test
    void adminXemDanhSachMacDinhTrang0Co20VaKhongTimKiem() throws Exception {
        when(service.list(isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(nguoiDung(true)), 0, 20, 1));

        mockMvc.perform(get("/api/admin/users").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].email").value(EMAIL_HS))
                .andExpect(jsonPath("$.items[0].displayName").value("Học sinh"))
                .andExpect(jsonPath("$.items[0].role").value("LEARNER"))
                .andExpect(jsonPath("$.items[0].authProvider").value("LOCAL"))
                .andExpect(jsonPath("$.items[0].enabled").value(true))
                .andExpect(content().string(not(containsString("password"))));

        verify(service).list(null, 0, 20);
    }

    @Test
    void thamSoQPageSizeDuocChuyenChoService() throws Exception {
        when(service.list(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 2, 10, 0));

        mockMvc.perform(get("/api/admin/users?q=nguyen&page=2&size=10").with(user(admin())))
                .andExpect(status().isOk());

        verify(service).list("nguyen", 2, 10);
    }

    @Test
    void phanTrangSaiThiBi400VaServiceKhongDuocGoi() throws Exception {
        for (String query : List.of("page=-1", "size=0", "size=51", "page=abc", "size=abc")) {
            mockMvc.perform(get("/api/admin/users?" + query).with(user(admin())))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(service);
    }

    @Test
    void tuKhoaTimKiemQuaDai101KyTuThiBi400() throws Exception {
        mockMvc.perform(get("/api/admin/users?q=" + "a".repeat(101)).with(user(admin())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---- Khóa / mở khóa ----

    // Người thực hiện lấy từ session, không từ trình duyệt: thân yêu cầu có "actorId" cũng bị bỏ qua.
    @Test
    void khoaTaiKhoanThiGoiServiceVoiIdAdminTuSessionVaXoaMoiSessionCuaNguoiBiKhoa() throws Exception {
        when(service.changeEnabled(ADMIN_ID, 7L, false)).thenReturn(nguoiDung(false));

        mockMvc.perform(patch("/api/admin/users/7/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false,\"actorId\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(service).changeEnabled(ADMIN_ID, 7L, false);
        verify(revoker).revokeAll(EMAIL_HS);
    }

    @Test
    void moKhoaTaiKhoanThiKhongXoaSessionNao() throws Exception {
        when(service.changeEnabled(ADMIN_ID, 7L, true)).thenReturn(nguoiDung(true));

        mockMvc.perform(patch("/api/admin/users/7/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(revoker, never()).revokeAll(any());
    }

    @Test
    void thanKhoaThieuHoacSaiKieuEnabledThiBi400() throws Exception {
        for (String body : List.of("{}", "{\"enabled\":null}", "{\"enabled\":\"khoa\"}", "")) {
            mockMvc.perform(patch("/api/admin/users/7/status").with(user(admin())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(service, revoker);
    }

    @Test
    void khoaBiTuChoiThi409VoiLyDoVaKhongXoaSession() throws Exception {
        when(service.changeEnabled(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(UserActionNotAllowedException.selfDisable());

        mockMvc.perform(patch("/api/admin/users/1/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(UserActionNotAllowedException.selfDisable().getMessage()));

        verifyNoInteractions(revoker);
    }

    @Test
    void khoaAdminCuoiCungBi409VoiLyDoRieng() throws Exception {
        when(service.changeEnabled(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(UserActionNotAllowedException.lastAdmin());

        mockMvc.perform(patch("/api/admin/users/2/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(UserActionNotAllowedException.lastAdmin().getMessage()));

        verifyNoInteractions(revoker);
    }

    @Test
    void khoaTaiKhoanKhongTonTaiThi404VoiCauCoDinh() throws Exception {
        when(service.changeEnabled(anyLong(), anyLong(), anyBoolean())).thenThrow(new UserNotFoundException());

        mockMvc.perform(patch("/api/admin/users/99/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy người dùng"));

        verifyNoInteractions(revoker);
    }

    @Test
    void idKhongPhaiSoThi400() throws Exception {
        mockMvc.perform(patch("/api/admin/users/abc/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/users/abc/reset-password").with(user(admin())).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service, revoker);
    }

    // ---- Đặt lại mật khẩu ----

    @Test
    void datLaiMatKhauTraMatKhauTamKhongChoPhepLuuCacheVaXoaMoiSession() throws Exception {
        when(service.resetPassword(ADMIN_ID, 7L))
                .thenReturn(new ResetPasswordResponse(7L, EMAIL_HS, "Abcd2345Efgh6789"));

        mockMvc.perform(post("/api/admin/users/7/reset-password").with(user(admin())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value(EMAIL_HS))
                .andExpect(jsonPath("$.temporaryPassword").value("Abcd2345Efgh6789"));

        verify(service).resetPassword(ADMIN_ID, 7L);
        verify(revoker).revokeAll(EMAIL_HS);
    }

    @Test
    void datLaiMatKhauBiTuChoiThi409KhongXoaSessionVaKhongCoMatKhauTrongThan() throws Exception {
        when(service.resetPassword(anyLong(), anyLong())).thenThrow(UserActionNotAllowedException.noPassword());

        mockMvc.perform(post("/api/admin/users/7/reset-password").with(user(admin())).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(UserActionNotAllowedException.noPassword().getMessage()))
                .andExpect(content().string(not(containsString("temporaryPassword"))));

        verifyNoInteractions(revoker);
    }

    @Test
    void datLaiMatKhauTaiKhoanKhongTonTaiThi404() throws Exception {
        when(service.resetPassword(anyLong(), anyLong())).thenThrow(new UserNotFoundException());

        mockMvc.perform(post("/api/admin/users/99/reset-password").with(user(admin())).with(csrf()))
                .andExpect(status().isNotFound());

        verifyNoInteractions(revoker);
    }
}
