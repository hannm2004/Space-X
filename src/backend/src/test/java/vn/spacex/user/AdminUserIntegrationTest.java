package vn.spacex.user;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng với MySQL 8.4 thật: ADMIN tạo lúc khởi động, session thật trong bảng spring_session.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminUserIntegrationTest {

    private static final String ADMIN_EMAIL = "quan.tri@example.com";
    private static final String ADMIN_MAT_KHAU = "mat-khau-quan-tri-1";
    private static final String HS_EMAIL = "hs@example.com";
    private static final String HS_MAT_KHAU = "matkhau123";

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinh(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("app.admin.email", () -> ADMIN_EMAIL);
        registry.add("app.admin.password", () -> ADMIN_MAT_KHAU);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private long adminId;

    // Không xóa ADMIN đầu tiên (tạo một lần lúc khởi động); xóa mọi tài khoản khác, kể cả admin phụ.
    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lesson_progress");
        jdbc.update("DELETE FROM users WHERE email <> ?", ADMIN_EMAIL);
        jdbc.update("UPDATE users SET enabled = TRUE WHERE email = ?", ADMIN_EMAIL);
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, ADMIN_EMAIL);
    }

    private Cookie dangNhap(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return result.getResponse().getCookie("SESSION");
    }

    private int thuDangNhap(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))).andReturn()
                .getResponse().getStatus();
    }

    private long dangKy(String email, String ten) throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","email":"%s","password":"%s","confirmPassword":"%s"}"""
                                .formatted(ten, email, HS_MAT_KHAU, HS_MAT_KHAU)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private int trangThaiMe(Cookie session) throws Exception {
        return mockMvc.perform(get("/api/me").cookie(session)).andReturn().getResponse().getStatus();
    }

    private void khoa(Cookie admin, long id, boolean enabled, int mongDoi) throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + id + "/status").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":" + enabled + "}"))
                .andExpect(status().is(mongDoi));
    }

    private boolean dangBat(long id) {
        return jdbc.queryForObject("SELECT enabled FROM users WHERE id = ?", Boolean.class, id);
    }

    // ---- Phân quyền ----

    @Test
    void khachVaHocSinhKhongGoiDuocApiQuanLyNguoiDungVaDatabaseKhongDoi() throws Exception {
        long hsId = dangKy(HS_EMAIL, "Học sinh");
        Cookie hs = dangNhap(HS_EMAIL, HS_MAT_KHAU);
        String hashTruoc = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, hsId);

        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users").cookie(hs)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/users/" + hsId + "/status").cookie(hs).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/" + hsId + "/reset-password").cookie(hs).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(dangBat(hsId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, hsId))
                .isEqualTo(hashTruoc);
    }

    // ---- Danh sách và tìm kiếm ----

    @Test
    void danhSachCoDuTruongKhongLoMaBamVaSapMoiNhatTruoc() throws Exception {
        dangKy("a@example.com", "An");
        long bId = dangKy("b@example.com", "Bình");
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(get("/api/admin/users").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].id").value(bId))
                .andExpect(jsonPath("$.items[0].email").value("b@example.com"))
                .andExpect(jsonPath("$.items[0].role").value("LEARNER"))
                .andExpect(jsonPath("$.items[0].enabled").value(true))
                .andExpect(jsonPath("$.items[2].role").value("ADMIN"))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void timTheoMotPhanEmailHoacTenKhongPhanBietHoaThuongVaPhanTrang() throws Exception {
        dangKy("nguyen.van.a@example.com", "Văn A");
        dangKy("hs2@example.com", "Trần NGUYEN Bình");
        dangKy("khac@example.com", "Khác");
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(get("/api/admin/users?q=Nguyen").cookie(admin))
                .andExpect(jsonPath("$.totalItems").value(2));
        mockMvc.perform(get("/api/admin/users?q=nguyen&size=1").cookie(admin))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)));
        mockMvc.perform(get("/api/admin/users?q=khongcoai").cookie(admin))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    // "%" và "_" phải được hiểu là ký tự thường, không khớp mọi tài khoản.
    @Test
    void tuKhoaPhanTramVaGachDuoiKhongKhopMoiTaiKhoan() throws Exception {
        dangKy("a@example.com", "An");
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(get("/api/admin/users?q=%25").cookie(admin)).andExpect(jsonPath("$.totalItems").value(0));
        mockMvc.perform(get("/api/admin/users?q=_").cookie(admin)).andExpect(jsonPath("$.totalItems").value(0));
    }

    // ---- Khóa / mở khóa ----

    @Test
    void khoaHocSinhThiKhongDangNhapDuocVaSessionDangMoCungBiDangXuat() throws Exception {
        long hsId = dangKy(HS_EMAIL, "Học sinh");
        Cookie hs = dangNhap(HS_EMAIL, HS_MAT_KHAU);
        Cookie hsMayKhac = dangNhap(HS_EMAIL, HS_MAT_KHAU);
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);
        assertThat(trangThaiMe(hs)).isEqualTo(200);

        khoa(admin, hsId, false, 200);

        assertThat(dangBat(hsId)).isFalse();
        assertThat(trangThaiMe(hs)).isEqualTo(401);
        assertThat(trangThaiMe(hsMayKhac)).isEqualTo(401);
        assertThat(thuDangNhap(HS_EMAIL, HS_MAT_KHAU)).isEqualTo(401);
        assertThat(trangThaiMe(admin)).isEqualTo(200);
    }

    @Test
    void khoaXongMoKhoaThiDangNhapLaiDuocVaSessionCuaAdminKhongBiAnhHuong() throws Exception {
        long hsId = dangKy(HS_EMAIL, "Học sinh");
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);
        khoa(admin, hsId, false, 200);

        khoa(admin, hsId, true, 200);

        assertThat(dangBat(hsId)).isTrue();
        assertThat(thuDangNhap(HS_EMAIL, HS_MAT_KHAU)).isEqualTo(200);
        assertThat(trangThaiMe(admin)).isEqualTo(200);
    }

    @Test
    void adminKhongTuKhoaMinhVaVanDangNhapDuoc() throws Exception {
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(patch("/api/admin/users/" + adminId + "/status").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("tự khóa")));

        assertThat(dangBat(adminId)).isTrue();
        assertThat(trangThaiMe(admin)).isEqualTo(200);
    }

    // Khóa admin phụ được vì còn admin đang hoạt động (người gọi); admin phụ bị khóa thì cũng bị đăng xuất.
    @Test
    void khoaAdminKhacKhiConAdminHoatDongThiDuocVaAdminBiKhoaBiDangXuat() throws Exception {
        long admin2Id = taoAdminPhu("admin2@example.com");
        Cookie admin2 = dangNhap("admin2@example.com", HS_MAT_KHAU);
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        khoa(admin, admin2Id, false, 200);

        assertThat(dangBat(admin2Id)).isFalse();
        assertThat(trangThaiMe(admin2)).isEqualTo(401);
    }

    // Admin phụ đang hoạt động nhưng admin gốc đã bị khóa: admin phụ là admin cuối cùng nên không ai khóa được nó.
    @Test
    void khongKhoaDuocAdminCuoiCungConHoatDongKhiPhienCuaNguoiGoiConSong() throws Exception {
        long admin2Id = taoAdminPhu("admin2@example.com");
        Cookie admin2 = dangNhap("admin2@example.com", HS_MAT_KHAU);
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);
        // Mô phỏng phiên cũ còn sống của admin gốc dù tài khoản đã bị khóa trực tiếp trong database.
        jdbc.update("UPDATE users SET enabled = FALSE WHERE id = ?", adminId);

        mockMvc.perform(patch("/api/admin/users/" + admin2Id + "/status").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Admin cuối cùng")));

        assertThat(dangBat(admin2Id)).isTrue();
        assertThat(trangThaiMe(admin2)).isEqualTo(200);
    }

    @Test
    void khoaTaiKhoanKhongTonTaiThi404() throws Exception {
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        khoa(admin, 999999, false, 404);
    }

    // ---- Đặt lại mật khẩu ----

    @Test
    void datLaiMatKhauThiMatKhauTamDangNhapDuocMatKhauCuKhongVaMoiSessionCuBiXoa() throws Exception {
        long hsId = dangKy(HS_EMAIL, "Học sinh");
        Cookie hsCu = dangNhap(HS_EMAIL, HS_MAT_KHAU);
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        MvcResult result = mockMvc.perform(post("/api/admin/users/" + hsId + "/reset-password")
                        .cookie(admin).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(HS_EMAIL))
                .andReturn();
        String matKhauTam = JsonPath.read(result.getResponse().getContentAsString(), "$.temporaryPassword");

        assertThat(matKhauTam).hasSize(16);
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(thuDangNhap(HS_EMAIL, HS_MAT_KHAU)).isEqualTo(401);
        assertThat(trangThaiMe(hsCu)).isEqualTo(401);
        assertThat(thuDangNhap(HS_EMAIL, matKhauTam)).isEqualTo(200);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, hsId);
        assertThat(hash).isNotEqualTo(matKhauTam);
        assertThat(passwordEncoder.matches(matKhauTam, hash)).isTrue();
    }

    @Test
    void moiLanDatLaiRaMotMatKhauTamKhac() throws Exception {
        long hsId = dangKy(HS_EMAIL, "Học sinh");
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        String lan1 = datLai(admin, hsId);
        String lan2 = datLai(admin, hsId);

        assertThat(lan1).isNotEqualTo(lan2);
        assertThat(thuDangNhap(HS_EMAIL, lan1)).isEqualTo(401);
        assertThat(thuDangNhap(HS_EMAIL, lan2)).isEqualTo(200);
    }

    @Test
    void adminKhongTuDatLaiMatKhauCuaMinhVaMatKhauKhongDoi() throws Exception {
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);
        String hashTruoc = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, adminId);

        mockMvc.perform(post("/api/admin/users/" + adminId + "/reset-password").cookie(admin).with(csrf()))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, adminId))
                .isEqualTo(hashTruoc);
        assertThat(trangThaiMe(admin)).isEqualTo(200);
    }

    @Test
    void taiKhoanGoogleKhongCoMatKhauThiKhongDatLaiDuocVaVanKhongCoMatKhau() throws Exception {
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, provider_id, enabled)
                VALUES ('g@example.com', NULL, 'Google', 'LEARNER', 'GOOGLE', 'gid-1', TRUE)""");
        long gId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'g@example.com'", Long.class);
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(post("/api/admin/users/" + gId + "/reset-password").cookie(admin).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Google")));

        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, gId)).isNull();
    }

    @Test
    void datLaiMatKhauTaiKhoanKhongTonTaiThi404() throws Exception {
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(post("/api/admin/users/999999/reset-password").cookie(admin).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private String datLai(Cookie admin, long id) throws Exception {
        String json = mockMvc.perform(post("/api/admin/users/" + id + "/reset-password").cookie(admin).with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.temporaryPassword");
    }

    // Không có API tạo admin thứ hai (chỉ AdminBootstrap), nên thêm thẳng vào database cho test.
    private long taoAdminPhu(String email) {
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, enabled)
                VALUES (?, ?, 'Admin phụ', 'ADMIN', 'LOCAL', TRUE)""", email, passwordEncoder.encode(HS_MAT_KHAU));
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }
}
