package vn.spacex.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.health.HealthController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Có GOOGLE_CLIENT_ID: đường dẫn đăng nhập Google hoạt động. Không gọi Google thật: bước chuyển hướng chỉ dựng địa chỉ.
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, GoogleLoginConfig.class, GoogleLoginSuccessHandler.class,
        GoogleLoginFailureHandler.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"app.google.client-id=test-client-id", "app.google.client-secret=test-client-secret"})
class GoogleLoginConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleAccountService accountService;

    @Test
    void duongDanDangNhapGoogleChuyenSangGoogleVoiDungThamSo() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound())
                .andReturn();

        String location = URLDecoder.decode(result.getResponse().getHeader("Location"), StandardCharsets.UTF_8);
        assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(location).contains("client_id=test-client-id");
        assertThat(location).contains("response_type=code");
        assertThat(location).contains("scope=openid email profile");
        assertThat(location).contains("redirect_uri=http://localhost/login/oauth2/code/google");
        // state chống giả mạo yêu cầu (CSRF của luồng OAuth2) do Spring tạo ngẫu nhiên.
        assertThat(location).containsPattern("state=[A-Za-z0-9_\\-.~=]{10,}");
    }

    @Test
    void khoaBiMatCuaUngDungKhongBaoGioXuatHienTrenDiaChiChuyenHuong() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")).andReturn();

        assertThat(result.getResponse().getHeader("Location")).doesNotContain("test-client-secret");
    }

    // Google gọi lại mà không có yêu cầu đăng nhập trước đó (state lạ): coi là thất bại chung, không gọi Google.
    @Test
    void goiLaiVoiStateLaThiVeTrangDangNhapVoiMaThatBai() throws Exception {
        mockMvc.perform(get("/login/oauth2/code/google").param("code", "x").param("state", "gia-mao"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dang-nhap?loi=google-that-bai"));
    }

    @Test
    void apiVanDoiDangNhapNhuCu() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}

// Không có GOOGLE_CLIENT_ID (máy chưa cấu hình, hoặc Render trước khi đặt biến): ứng dụng vẫn chạy, tính năng tắt.
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, GoogleLoginConfig.class, GoogleLoginSuccessHandler.class,
        GoogleLoginFailureHandler.class, GlobalExceptionHandler.class})
// Phải ép rỗng: application.properties nạp ../../.env ở máy, nên nếu .env có GOOGLE_CLIENT_ID thì tính năng sẽ bật và
// test này sai theo máy (CI không có .env nên không lộ ra).
@TestPropertySource(properties = "app.google.client-id=")
class GoogleLoginDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleAccountService accountService;

    @Test
    void khongCoClientIdThiKhongCoDuongDanDangNhapGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google")).andExpect(header().doesNotExist("Location"));
        mockMvc.perform(get("/login/oauth2/code/google").param("code", "x").param("state", "y"))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void apiVanDoiDangNhapNhuCu() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}
