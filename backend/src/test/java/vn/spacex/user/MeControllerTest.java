package vn.spacex.user;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MeControllerTest {

    private static final long USER_ID = 7L;

    private static final String DOI_MAT_KHAU_HOP_LE = """
            {"currentPassword":"matkhau-cu-1","newPassword":"matkhau-moi-2","confirmNewPassword":"matkhau-moi-2"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeService meService;

    private static AppUserDetails nguoiDung() {
        return new AppUserDetails(USER_ID, "a@example.com", null, "Nguyễn Văn A", Role.LEARNER, true);
    }

    private static MeResponse hoSo(String ten) {
        return new MeResponse(ten, "a@example.com", Role.LEARNER, AuthProvider.LOCAL,
                LocalDateTime.of(2026, 11, 1, 9, 30));
    }

    @Test
    void chuaDangNhapXemHoSoBi401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());

        verify(meService, never()).getProfile(any());
    }

    @Test
    void xemHoSoTraVeThongTinCuaNguoiDangNhapKhongLoMaBamMatKhau() throws Exception {
        when(meService.getProfile(USER_ID)).thenReturn(hoSo("Nguyễn Văn A"));

        mockMvc.perform(get("/api/me").with(user(nguoiDung())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.email").value("a@example.com"))
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.authProvider").value("LOCAL"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    // Người dùng hiện tại luôn lấy từ session (CLAUDE.md): userId gửi lên từ trình duyệt bị bỏ qua.
    @Test
    void userIdTrenDuongDanBiBoQuaChiDungIdTuSession() throws Exception {
        when(meService.getProfile(USER_ID)).thenReturn(hoSo("Nguyễn Văn A"));

        mockMvc.perform(get("/api/me").param("userId", "999").with(user(nguoiDung())))
                .andExpect(status().isOk());

        verify(meService).getProfile(USER_ID);
        verify(meService, never()).getProfile(999L);
    }

    @Test
    void suaTenHopLeGoiServiceVoiIdTuSessionVaBoQuaRoleEmailId() throws Exception {
        when(meService.updateProfile(eq(USER_ID), any())).thenReturn(hoSo("An"));

        mockMvc.perform(put("/api/me").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"  An  ","id":999,"role":"ADMIN","email":"hacker@example.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("An"));

        ArgumentCaptor<UpdateProfileRequest> captor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(meService).updateProfile(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().displayName()).isEqualTo("An");
    }

    @Test
    void suaTenTrongBi400KemDanhSachTruongLoi() throws Exception {
        mockMvc.perform(put("/api/me").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"displayName":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("displayName"));

        verify(meService, never()).updateProfile(any(), any());
    }

    @Test
    void suaTenThieuCsrfBi403() throws Exception {
        mockMvc.perform(put("/api/me").with(user(nguoiDung()))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"displayName":"An"}"""))
                .andExpect(status().isForbidden());

        verify(meService, never()).updateProfile(any(), any());
    }

    @Test
    void chuaDangNhapSuaTenBi401() throws Exception {
        mockMvc.perform(put("/api/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"displayName":"An"}"""))
                .andExpect(status().isUnauthorized());

        verify(meService, never()).updateProfile(any(), any());
    }

    @Test
    void doiMatKhauHopLeTra204VaDungIdTuSession() throws Exception {
        mockMvc.perform(put("/api/me/password").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DOI_MAT_KHAU_HOP_LE))
                .andExpect(status().isNoContent());

        verify(meService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
    }

    @Test
    void doiMatKhauDuLieuSaiBi400KhongLoMatKhau() throws Exception {
        mockMvc.perform(put("/api/me/password").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"currentPassword":"matkhau-cu-1","newPassword":"matkhau-moi-2","confirmNewPassword":"matkhau-khac-3"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("confirmNewPasswordMatching"))
                .andExpect(content().string(not(containsString("matkhau-cu-1"))))
                .andExpect(content().string(not(containsString("matkhau-moi-2"))))
                .andExpect(content().string(not(containsString("matkhau-khac-3"))));

        verify(meService, never()).changePassword(any(), any());
    }

    @Test
    void saiMatKhauCuBi400() throws Exception {
        doThrow(new WrongCurrentPasswordException()).when(meService).changePassword(eq(USER_ID), any());

        mockMvc.perform(put("/api/me/password").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DOI_MAT_KHAU_HOP_LE))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Mật khẩu hiện tại không đúng"))
                .andExpect(content().string(not(containsString("matkhau-cu-1"))));
    }

    @Test
    void taiKhoanKhongCoMatKhauBi400() throws Exception {
        doThrow(new NoPasswordAccountException()).when(meService).changePassword(eq(USER_ID), any());

        mockMvc.perform(put("/api/me/password").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DOI_MAT_KHAU_HOP_LE))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void chuaDangNhapDoiMatKhauBi401() throws Exception {
        mockMvc.perform(put("/api/me/password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DOI_MAT_KHAU_HOP_LE))
                .andExpect(status().isUnauthorized());

        verify(meService, never()).changePassword(any(), any());
    }
}
