package vn.spacex.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.user.Role;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    private static final String DANG_KY_HOP_LE = """
            {"displayName":"Nguyễn Văn A","email":"a@example.com","password":"matkhau123","confirmPassword":"matkhau123"}""";

    private static final String DANG_NHAP = """
            {"email":"a@example.com","password":"matkhau123"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void dangKyHopLeTraVe201() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DANG_KY_HOP_LE))
                .andExpect(status().isCreated());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void dangKyThieuCsrfTraVe403() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(DANG_KY_HOP_LE))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any());
    }

    @Test
    void dangKyDuLieuSaiTraVe400KemDanhSachTruongLoiKhongLoMatKhau() throws Exception {
        String saiXacNhan = """
                {"displayName":"A","email":"a@example.com","password":"matkhau123","confirmPassword":"matkhau-khac"}""";

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(saiXacNhan))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("confirmPasswordMatching"))
                .andExpect(content().string(not(containsString("matkhau123"))))
                .andExpect(content().string(not(containsString("matkhau-khac"))));

        verify(authService, never()).register(any());
    }

    @Test
    void dangKyEmailTrungTraVe409() throws Exception {
        doThrow(new EmailAlreadyExistsException()).when(authService).register(any());

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DANG_KY_HOP_LE))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void dangNhapThanhCongTraVe200KemThongTinNguoiDung() throws Exception {
        AppUserDetails principal = new AppUserDetails(1L, "a@example.com", "hash", "Nguyễn Văn A", Role.LEARNER, true);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DANG_NHAP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.email").value("a@example.com"))
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void dangNhapThatBaiDuLoiNaoCungTraVe401ChungKhongLoLyDo() throws Exception {
        for (RuntimeException loi : List.of(
                new BadCredentialsException("sai mat khau"),
                new DisabledException("tai khoan bi khoa"))) {
            doThrow(loi).when(authenticationManager).authenticate(any());

            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(DANG_NHAP))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Email hoặc mật khẩu không chính xác"))
                    .andExpect(content().string(not(containsString(loi.getMessage()))));
        }
    }

    @Test
    void loiHeThongKhiXacThucLaMotLoi500KhongPhaiMot401() throws Exception {
        // Database sập không có nghĩa là người dùng gõ sai mật khẩu.
        when(authenticationManager.authenticate(any()))
                .thenThrow(new InternalAuthenticationServiceException("mat ket noi database"));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DANG_NHAP))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("database"))));
    }

    @Test
    void dangNhapThieuTruongTraVe400() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email":"a@example.com"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dangNhapThieuCsrfTraVe403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(DANG_NHAP))
                .andExpect(status().isForbidden());
    }
}
