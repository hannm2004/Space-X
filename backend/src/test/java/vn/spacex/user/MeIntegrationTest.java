package vn.spacex.user;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Luồng thật như trình duyệt: cookie XSRF-TOKEN + header, cookie SESSION trong MySQL, dữ liệu thật trong bảng users.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MeIntegrationTest {

    private static final String EMAIL_A = "a@example.com";
    private static final String EMAIL_B = "b@example.com";
    private static final String MAT_KHAU = "matkhau-cu-1";

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinhDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM users");
    }

    private Cookie layXsrf() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/api/health")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        return xsrf;
    }

    private MvcResult guiJson(HttpMethod method, String url, String json, Cookie session) throws Exception {
        Cookie xsrf = layXsrf();
        var builder = request(method, url).contentType(MediaType.APPLICATION_JSON).content(json)
                .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue());
        if (session != null) {
            builder = builder.cookie(session);
        }
        return mockMvc.perform(builder).andReturn();
    }

    private void dangKy(String email, String ten) throws Exception {
        MvcResult result = guiJson(HttpMethod.POST, "/api/auth/register", """
                {"displayName":"%s","email":"%s","password":"%s","confirmPassword":"%s"}"""
                .formatted(ten, email, MAT_KHAU, MAT_KHAU), null);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
    }

    private MvcResult dangNhap(String email, String matKhau) throws Exception {
        return guiJson(HttpMethod.POST, "/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, matKhau), null);
    }

    private Cookie dangKyVaDangNhap(String email, String ten) throws Exception {
        dangKy(email, ten);
        Cookie session = dangNhap(email, MAT_KHAU).getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();
        return session;
    }

    private String tenTrongDb(String email) {
        return jdbc.queryForObject("SELECT display_name FROM users WHERE email = ?", String.class, email);
    }

    private String hashTrongDb(String email) {
        return jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
    }

    @Test
    void chuaDangNhapXemHoSoBi401() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void xemHoSoChiThayThongTinCuaMinhKhongLoMaBam() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");
        dangKy(EMAIL_B, "Bình");

        mockMvc.perform(get("/api/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("An"))
                .andExpect(jsonPath("$.email").value(EMAIL_A))
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.authProvider").value("LOCAL"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(content().string(not(containsString(EMAIL_B))))
                .andExpect(content().string(not(containsString("$2"))));
    }

    @Test
    void suaTenLuuVaoDatabaseChiChoMinhVaXemLaiThayTenMoi() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");
        dangKy(EMAIL_B, "Bình");

        MvcResult result = guiJson(HttpMethod.PUT, "/api/me", """
                {"displayName":"An Mới"}""", session);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(tenTrongDb(EMAIL_A)).isEqualTo("An Mới");
        assertThat(tenTrongDb(EMAIL_B)).isEqualTo("Bình");
        mockMvc.perform(get("/api/me").cookie(session))
                .andExpect(jsonPath("$.displayName").value("An Mới"));
    }

    @Test
    void suaTenKhongDoiDuocEmailRoleHayTaiKhoanKhac() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");
        dangKy(EMAIL_B, "Bình");
        Long idB = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, EMAIL_B);

        guiJson(HttpMethod.PUT, "/api/me", """
                {"displayName":"An Mới","id":%d,"email":"hacker@example.com","role":"ADMIN"}""".formatted(idB), session);

        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email = ?", String.class, EMAIL_A)).isEqualTo("LEARNER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, "hacker@example.com")).isZero();
        assertThat(tenTrongDb(EMAIL_B)).isEqualTo("Bình");
    }

    @Test
    void suaTenTrongBi400VaKhongDoiDuLieu() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");

        MvcResult result = guiJson(HttpMethod.PUT, "/api/me", """
                {"displayName":"   "}""", session);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(tenTrongDb(EMAIL_A)).isEqualTo("An");
    }

    @Test
    void suaTenThieuCsrfBi403() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");

        mockMvc.perform(request(HttpMethod.PUT, "/api/me").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"displayName":"An Mới"}"""))
                .andExpect(status().isForbidden());

        assertThat(tenTrongDb(EMAIL_A)).isEqualTo("An");
    }

    @Test
    void doiMatKhauThanhCongThiMatKhauCuHetHieuLucVaMatKhauMoiDangNhapDuoc() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");

        MvcResult result = guiJson(HttpMethod.PUT, "/api/me/password", """
                {"currentPassword":"%s","newPassword":"matkhau-moi-2","confirmNewPassword":"matkhau-moi-2"}"""
                .formatted(MAT_KHAU), session);

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(hashTrongDb(EMAIL_A)).startsWith("$2").doesNotContain("matkhau-moi-2");
        assertThat(dangNhap(EMAIL_A, MAT_KHAU).getResponse().getStatus()).isEqualTo(401);
        assertThat(dangNhap(EMAIL_A, "matkhau-moi-2").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void saiMatKhauCuBi400VaMatKhauKhongDoi() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");
        String hashTruoc = hashTrongDb(EMAIL_A);

        MvcResult result = guiJson(HttpMethod.PUT, "/api/me/password", """
                {"currentPassword":"sai-mat-khau","newPassword":"matkhau-moi-2","confirmNewPassword":"matkhau-moi-2"}""",
                session);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("sai-mat-khau").doesNotContain("matkhau-moi-2");
        assertThat(hashTrongDb(EMAIL_A)).isEqualTo(hashTruoc);
        assertThat(dangNhap(EMAIL_A, MAT_KHAU).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void doiMatKhauChiAnhHuongTaiKhoanCuaMinh() throws Exception {
        Cookie session = dangKyVaDangNhap(EMAIL_A, "An");
        dangKy(EMAIL_B, "Bình");
        String hashB = hashTrongDb(EMAIL_B);

        guiJson(HttpMethod.PUT, "/api/me/password", """
                {"currentPassword":"%s","newPassword":"matkhau-moi-2","confirmNewPassword":"matkhau-moi-2"}"""
                .formatted(MAT_KHAU), session);

        assertThat(hashTrongDb(EMAIL_B)).isEqualTo(hashB);
        assertThat(dangNhap(EMAIL_B, MAT_KHAU).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void chuaDangNhapDoiMatKhauBi401() throws Exception {
        MvcResult result = guiJson(HttpMethod.PUT, "/api/me/password", """
                {"currentPassword":"a","newPassword":"matkhau-moi-2","confirmNewPassword":"matkhau-moi-2"}""", null);

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}
