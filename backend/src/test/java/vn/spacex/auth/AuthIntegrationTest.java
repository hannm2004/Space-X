package vn.spacex.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AuthIntegrationTest.BaoVeController.class)
class AuthIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinhDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    // Endpoint chỉ tồn tại trong test, đại diện cho mọi API cần đăng nhập; không đưa vào mã thật.
    @RestController
    static class BaoVeController {

        @GetMapping("/api/test/bao-ve")
        String baoVe() {
            return "ok";
        }
    }

    private static final String LOI_CHUNG = "Email hoặc mật khẩu không chính xác";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void xoaTaiKhoan() {
        jdbc.update("DELETE FROM users");
    }

    private MvcResult dangKy(String displayName, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"displayName":"%s","email":"%s","password":"%s","confirmPassword":"%s"}"""
                        .formatted(displayName, email, password, password))).andReturn();
    }

    private MvcResult dangNhap(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))).andReturn();
    }

    @Test
    void dangKyLuuEmailChuThuongVaMatKhauDaBamBcrypt() throws Exception {
        MvcResult result = dangKy("Nguyễn Văn A", "  A@Example.COM ", "matkhau123");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT email, password_hash, display_name, role, auth_provider, provider_id, enabled FROM users");
        assertThat(row.get("email")).isEqualTo("a@example.com");
        assertThat(row.get("display_name")).isEqualTo("Nguyễn Văn A");
        assertThat(row.get("role")).isEqualTo("LEARNER");
        assertThat(row.get("auth_provider")).isEqualTo("LOCAL");
        assertThat(row.get("provider_id")).isNull();
        assertThat((String) row.get("password_hash")).startsWith("$2").isNotEqualTo("matkhau123");
    }

    @Test
    void dangKyBoQuaTruongRoleGuiLen() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Hacker","email":"h@example.com","password":"matkhau123",
                                 "confirmPassword":"matkhau123","role":"ADMIN"}"""))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email='h@example.com'", String.class))
                .isEqualTo("LEARNER");
    }

    @Test
    void dangKyKhongTaoSessionVaKhongTuDangNhap() throws Exception {
        MvcResult result = dangKy("A", "a@example.com", "matkhau123");

        assertThat(result.getResponse().getCookie("SESSION")).isNull();
    }

    @Test
    void dangKyEmailTrungKhacHoaThuongHoacThuaKhoangTrangTraVe409() throws Exception {
        dangKy("A", "a@example.com", "matkhau123");

        assertThat(dangKy("B", "  A@EXAMPLE.com ", "matkhau123").getResponse().getStatus()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void dangKyBangEmailCuaTaiKhoanGoogleTraVe409() throws Exception {
        jdbc.update("""
                INSERT INTO users (email, display_name, auth_provider, provider_id)
                VALUES ('g@example.com', 'Google User', 'GOOGLE', 'google-123')""");

        assertThat(dangKy("B", "g@example.com", "matkhau123").getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void dangKyThieuCsrfTraVe403() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"A","email":"a@example.com","password":"matkhau123","confirmPassword":"matkhau123"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void dangNhapDungThongTinKeCaEmailVietHoaTraVe200VaCoSession() throws Exception {
        dangKy("Nguyễn Văn A", "a@example.com", "matkhau123");

        MvcResult result = dangNhap("  A@Example.COM ", "matkhau123");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("SESSION").doesNotContain("token").doesNotContain("$2");
        mockMvc.perform(get("/api/test/bao-ve").cookie(result.getResponse().getCookie("SESSION")))
                .andExpect(status().isOk());
    }

    @Test
    void dangNhapThanhCongTraVeDungBaTruong() throws Exception {
        dangKy("Nguyễn Văn A", "a@example.com", "matkhau123");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@example.com","password":"matkhau123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.email").value("a@example.com"))
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void moiTruongHopDangNhapThatBaiTraVeCungMotLoi401() throws Exception {
        dangKy("Dung", "dung@example.com", "matkhau123");
        dangKy("Khoa", "khoa@example.com", "matkhau123");
        jdbc.update("UPDATE users SET enabled = FALSE WHERE email = 'khoa@example.com'");
        jdbc.update("""
                INSERT INTO users (email, display_name, auth_provider, provider_id)
                VALUES ('g@example.com', 'Google User', 'GOOGLE', 'google-123')""");

        List<MvcResult> thatBai = List.of(
                dangNhap("khongco@example.com", "matkhau123"),   // email không tồn tại
                dangNhap("dung@example.com", "sai-mat-khau"),    // sai mật khẩu
                dangNhap("khoa@example.com", "matkhau123"),      // tài khoản bị khóa, mật khẩu đúng
                dangNhap("g@example.com", "matkhau123"));        // tài khoản Google không có mật khẩu

        String thanMau = thatBai.get(0).getResponse().getContentAsString();
        for (MvcResult result : thatBai) {
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
            assertThat(result.getResponse().getContentAsString()).isEqualTo(thanMau).contains(LOI_CHUNG);
            assertThat(result.getResponse().getCookie("SESSION")).isNull();
        }
    }

    @Test
    void dangNhapThieuCsrfTraVe403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@example.com","password":"matkhau123"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiCanDangNhapTraVe401KhiChuaDangNhap() throws Exception {
        mockMvc.perform(get("/api/test/bao-ve"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void dangXuatXongThiApiCanDangNhapTraVe401() throws Exception {
        dangKy("A", "a@example.com", "matkhau123");
        Cookie session = dangNhap("a@example.com", "matkhau123").getResponse().getCookie("SESSION");
        mockMvc.perform(get("/api/test/bao-ve").cookie(session)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").with(csrf()).cookie(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/test/bao-ve").cookie(session)).andExpect(status().isUnauthorized());
    }
}
