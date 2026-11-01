package vn.spacex.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Chạy cả ứng dụng với MySQL thật: ADMIN được tạo lúc khởi động từ biến môi trường, rồi đi qua luật /api/admin/**.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminAuthorizationIntegrationTest.GiaAdminController.class)
class AdminAuthorizationIntegrationTest {

    private static final String ADMIN_EMAIL = "quan.tri@example.com";
    private static final String ADMIN_MAT_KHAU = "mat-khau-quan-tri-1";

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinh(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        // Cố ý viết hoa và thừa khoảng trắng để kiểm tra chuẩn hóa email.
        registry.add("app.admin.email", () -> "  Quan.Tri@Example.COM ");
        registry.add("app.admin.password", () -> ADMIN_MAT_KHAU);
    }

    // Endpoint chỉ tồn tại trong test, đại diện cho API Admin; không đưa vào mã thật.
    @RestController
    static class GiaAdminController {

        @GetMapping("/api/admin/ping")
        String ping() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // Không xóa ADMIN: nó chỉ được tạo một lần lúc khởi động.
    @BeforeEach
    void xoaHocSinh() {
        jdbc.update("DELETE FROM users WHERE role = 'LEARNER'");
    }

    private MockHttpSession dangNhap(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void khoiDongTaoDungMotAdminVoiEmailChuanHoaVaMatKhauDaBam() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class))
                .isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT email, password_hash, auth_provider, enabled FROM users WHERE role = 'ADMIN'");
        assertThat(row.get("email")).isEqualTo(ADMIN_EMAIL);
        assertThat(row.get("auth_provider")).isEqualTo("LOCAL");
        assertThat((String) row.get("password_hash")).startsWith("$2").isNotEqualTo(ADMIN_MAT_KHAU);
    }

    @Test
    void adminDangNhapRoiGoiApiAdminThanhCong() throws Exception {
        MockHttpSession session = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(get("/api/admin/ping").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void hocSinhDangNhapRoiGoiApiAdminBi403() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Học sinh","email":"hs@example.com","password":"matkhau123",
                                 "confirmPassword":"matkhau123"}"""))
                .andExpect(status().isCreated());
        MockHttpSession session = dangNhap("hs@example.com", "matkhau123");

        mockMvc.perform(get("/api/admin/ping").session(session))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void chuaDangNhapGoiApiAdminBi401() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isUnauthorized());
    }
}
