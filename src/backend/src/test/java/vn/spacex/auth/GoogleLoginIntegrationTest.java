package vn.spacex.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import vn.spacex.user.User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng với MySQL 8.4 thật và đăng nhập Google đã bật (client id giả). Không gọi Google: phần xác thực với Google
// do Spring Security thực hiện; ở đây kiểm luật của ứng dụng trên database thật và việc session OAuth2 lưu được vào MySQL.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GoogleLoginIntegrationTest {

    private static final String SUB = "109876543210987654321";
    private static final String EMAIL = "hocsinh@gmail.com";

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinh(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("app.google.client-id", () -> "test-client-id");
        registry.add("app.google.client-secret", () -> "test-client-secret");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private GoogleAccountService service;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lesson_progress");
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM users");
    }

    private static GoogleProfile hoSo(String sub, String email) {
        return new GoogleProfile(sub, email, true, "Học Sinh");
    }

    private Map<String, Object> dongNguoiDung(String email) {
        return jdbc.queryForMap("SELECT * FROM users WHERE email = ?", email);
    }

    private int soNguoiDung() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    }

    @Test
    void dangNhapGoogleLanDauTaoDongDungTrongDatabase() {
        User user = service.signIn(hoSo(SUB, EMAIL));

        Map<String, Object> row = dongNguoiDung(EMAIL);
        assertThat(row.get("id")).isEqualTo(user.getId());
        assertThat(row.get("auth_provider")).isEqualTo("GOOGLE");
        assertThat(row.get("provider_id")).isEqualTo(SUB);
        assertThat(row.get("role")).isEqualTo("LEARNER");
        assertThat(row.get("password_hash")).isNull();
        assertThat(row.get("display_name")).isEqualTo("Học Sinh");
        assertThat((Boolean) row.get("enabled")).isTrue();
    }

    @Test
    void dangNhapLanHaiCungMaDinhDanhVaoDungTaiKhoanVaKhongTaoThem() {
        User lanDau = service.signIn(hoSo(SUB, EMAIL));
        User lanHai = service.signIn(hoSo(SUB, EMAIL));

        assertThat(lanHai.getId()).isEqualTo(lanDau.getId());
        assertThat(soNguoiDung()).isEqualTo(1);
    }

    @Test
    void dangNhapLanHaiVanVaoDuocKhiGoogleDoiEmailVaTaiKhoanKhongBiSua() {
        User lanDau = service.signIn(hoSo(SUB, EMAIL));

        User lanHai = service.signIn(hoSo(SUB, "email-moi@gmail.com"));

        assertThat(lanHai.getId()).isEqualTo(lanDau.getId());
        assertThat(lanHai.getEmail()).isEqualTo(EMAIL);
        assertThat(soNguoiDung()).isEqualTo(1);
    }

    @Test
    void emailVietHoaTuGoogleDuocLuuChuThuong() {
        service.signIn(hoSo(SUB, "  HocSinh@GMAIL.com "));

        assertThat(dongNguoiDung(EMAIL).get("email")).isEqualTo(EMAIL);
    }

    // D25: không tự liên kết. Tài khoản mật khẩu giữ nguyên, không có dòng mới.
    @Test
    void emailDaDangKyBangMatKhauBiTuChoiVaDatabaseKhongDoi() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Học Sinh","email":"%s","password":"matkhau123","confirmPassword":"matkhau123"}"""
                                .formatted(EMAIL)))
                .andExpect(status().isCreated());
        Map<String, Object> truoc = dongNguoiDung(EMAIL);

        assertThatThrownBy(() -> service.signIn(hoSo(SUB, EMAIL)))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class, e -> assertThat(e.reason())
                        .isEqualTo(GoogleLoginRejectedException.Reason.EMAIL_USED_BY_PASSWORD));

        assertThat(soNguoiDung()).isEqualTo(1);
        Map<String, Object> sau = dongNguoiDung(EMAIL);
        assertThat(sau.get("auth_provider")).isEqualTo("LOCAL");
        assertThat(sau.get("provider_id")).isNull();
        assertThat(sau.get("password_hash")).isEqualTo(truoc.get("password_hash"));
    }

    @Test
    void taiKhoanGoogleBiAdminKhoaThiKhongDangNhapDuocBangGoogle() {
        service.signIn(hoSo(SUB, EMAIL));
        jdbc.update("UPDATE users SET enabled = FALSE WHERE email = ?", EMAIL);

        assertThatThrownBy(() -> service.signIn(hoSo(SUB, EMAIL)))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class, e -> assertThat(e.reason())
                        .isEqualTo(GoogleLoginRejectedException.Reason.ACCOUNT_DISABLED));
    }

    // Tài khoản Google không có mật khẩu: đăng nhập bằng mật khẩu nhận cùng lỗi 401 chung như sai mật khẩu (D25).
    @Test
    void taiKhoanGoogleKhongDangNhapDuocBangMatKhau() throws Exception {
        service.signIn(hoSo(SUB, EMAIL));

        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"matkhau123"}""".formatted(EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    // Yêu cầu bắt đầu đăng nhập được lưu trong session; session lưu ở MySQL (D24) nên phải tuần tự hóa được.
    @Test
    void batDauDangNhapGoogleChuyenSangGoogleVaLuuDuocSessionVaoMysql() throws Exception {
        int truoc = jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class);

        MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getHeader("Location"))
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
                .contains("client_id=test-client-id");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isEqualTo(truoc + 1);
        assertThat(result.getResponse().getCookie("SESSION")).isNotNull();
    }
}
