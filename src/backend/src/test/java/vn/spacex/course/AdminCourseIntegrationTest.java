package vn.spacex.course;

import jakarta.servlet.http.Cookie;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng với MySQL 8.4 thật: ADMIN được tạo lúc khởi động, đăng nhập bằng session thật, rồi gọi API Admin.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminCourseIntegrationTest {

    private static final String ADMIN_EMAIL = "quan.tri@example.com";
    private static final String ADMIN_MAT_KHAU = "mat-khau-quan-tri-1";

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

    private long adminId;

    // Không xóa ADMIN: nó chỉ được tạo một lần lúc khởi động.
    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users WHERE role = 'LEARNER'");
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE role = 'ADMIN'", Long.class);
    }

    private Cookie dangNhap(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return result.getResponse().getCookie("SESSION");
    }

    private Cookie dangNhapAdmin() throws Exception {
        return dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);
    }

    private Cookie dangNhapHocSinh() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Học sinh","email":"hs@example.com","password":"matkhau123",
                                 "confirmPassword":"matkhau123"}"""))
                .andExpect(status().isCreated());
        return dangNhap("hs@example.com", "matkhau123");
    }

    private static String body(String slug, String title, String mota, int thuTu) {
        String moTaJson = mota == null ? "null" : "\"" + mota + "\"";
        return """
                {"slug":"%s","title":"%s","description":%s,"sortOrder":%d}""".formatted(slug, title, moTaJson, thuTu);
    }

    private long themKhoa(String slug, int thuTu) {
        jdbc.update("INSERT INTO courses (slug, title, description, sort_order) VALUES (?, ?, NULL, ?)",
                slug, "Khóa " + slug, thuTu);
        return jdbc.queryForObject("SELECT id FROM courses WHERE slug = ?", Long.class, slug);
    }

    private void themBai(long khoaId, String slug) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, 'Bài', 'Tóm tắt', 'Nội dung', 'PUBLISHED', 1)""", khoaId, adminId, slug);
    }

    private int demKhoa() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM courses", Integer.class);
    }

    // ---- Phân quyền ----

    @Test
    void khachGoiApiAdminKhoaHocBi401VaKhongGhiGiVaoDatabase() throws Exception {
        mockMvc.perform(post("/api/admin/courses").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("tin-hoc-10", "Tin học 10", null, 1)))
                .andExpect(status().isUnauthorized());

        assertThat(demKhoa()).isZero();
    }

    @Test
    void hocSinhDangNhapRoiTaoSuaXoaKhoaDeuBi403VaDatabaseKhongDoi() throws Exception {
        long id = themKhoa("co-san", 1);
        Cookie session = dangNhapHocSinh();

        mockMvc.perform(post("/api/admin/courses").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("moi", "Mới", null, 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/courses/" + id).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("da-sua", "Đã sửa", null, 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/courses/" + id).cookie(session).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(demKhoa()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT slug FROM courses WHERE id = ?", String.class, id))
                .isEqualTo("co-san");
    }

    @Test
    void adminDaDangNhapNhungThieuCsrfTokenBi403() throws Exception {
        Cookie session = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/courses").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body("tin-hoc-10", "Tin học 10", null, 1)))
                .andExpect(status().isForbidden());

        assertThat(demKhoa()).isZero();
    }

    // ---- Tạo ----

    @Test
    void adminTaoKhoaGhiDungVaoMySqlVaDanhSachCongKhaiThayNgay() throws Exception {
        Cookie session = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/courses").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("tin-hoc-10", "Tin học 10", "Chương trình lớp 10", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$.sortOrder").value(2));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT slug, title, description, sort_order FROM courses WHERE slug = 'tin-hoc-10'");
        assertThat(row.get("title")).isEqualTo("Tin học 10");
        assertThat(row.get("description")).isEqualTo("Chương trình lớp 10");
        assertThat(row.get("sort_order")).isEqualTo(2);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value("tin-hoc-10"));
    }

    @Test
    void tieuDeVaMoTaTiengVietCoDauDiQuaJsonVaMySqlKhongBiHong() throws Exception {
        Cookie session = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/courses").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("tin-hoc-12", "Tin học 12 — Hệ cơ sở dữ liệu", "Ứng dụng cho học sinh THPT", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tin học 12 — Hệ cơ sở dữ liệu"));

        assertThat(jdbc.queryForObject("SELECT title FROM courses WHERE slug = 'tin-hoc-12'", String.class))
                .isEqualTo("Tin học 12 — Hệ cơ sở dữ liệu");
    }

    @Test
    void taoKhoaTrungSlugBi409VaChiCoMotDong() throws Exception {
        themKhoa("tin-hoc-10", 1);
        Cookie session = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/courses").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("tin-hoc-10", "Khác", null, 5)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Slug khóa học đã tồn tại"));

        assertThat(demKhoa()).isEqualTo(1);
    }

    @Test
    void taoKhoaDuLieuSaiBi400VaKhongGhiGi() throws Exception {
        Cookie session = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/courses").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("Tin Hoc", "", null, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)));

        assertThat(demKhoa()).isZero();
    }

    // ---- Sửa ----

    @Test
    void adminSuaKhoaGhiDungVaoMySql() throws Exception {
        long id = themKhoa("tin-hoc-10", 1);
        Cookie session = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/courses/" + id).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("tin-hoc-10-moi", "Tin học 10 mới", "Mô tả mới", 9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.slug").value("tin-hoc-10-moi"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT slug, title, description, sort_order FROM courses WHERE id = ?", id);
        assertThat(row.get("slug")).isEqualTo("tin-hoc-10-moi");
        assertThat(row.get("title")).isEqualTo("Tin học 10 mới");
        assertThat(row.get("description")).isEqualTo("Mô tả mới");
        assertThat(row.get("sort_order")).isEqualTo(9);
    }

    @Test
    void suaKhoaGiuNguyenSlugCuaChinhNoThiThanhCongKhongBiBaoTrung() throws Exception {
        long id = themKhoa("tin-hoc-10", 1);
        Cookie session = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/courses/" + id).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("tin-hoc-10", "Đổi tiêu đề", null, 1)))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT title FROM courses WHERE id = ?", String.class, id))
                .isEqualTo("Đổi tiêu đề");
    }

    @Test
    void suaKhoaDoiSangSlugCuaKhoaKhacBi409VaCaHaiKhoaGiuNguyen() throws Exception {
        long khoa10 = themKhoa("tin-hoc-10", 1);
        themKhoa("tin-hoc-11", 2);
        Cookie session = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/courses/" + khoa10).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("tin-hoc-11", "Đổi", null, 1)))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT slug FROM courses WHERE id = ?", String.class, khoa10))
                .isEqualTo("tin-hoc-10");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE slug = 'tin-hoc-11'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void suaKhoaKhongTonTaiBi404() throws Exception {
        Cookie session = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/courses/999999").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("x", "X", null, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));
    }

    // ---- Xóa ----

    @Test
    void xoaKhoaKhongConBaiThiXoaThatKhoiMySql() throws Exception {
        long id = themKhoa("tin-hoc-10", 1);
        Cookie session = dangNhapAdmin();

        mockMvc.perform(delete("/api/admin/courses/" + id).cookie(session).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(demKhoa()).isZero();
    }

    @Test
    void xoaKhoaConBaiBi409KhoaVaBaiVanCon() throws Exception {
        long id = themKhoa("tin-hoc-10", 1);
        themBai(id, "bai-1");
        Cookie session = dangNhapAdmin();

        mockMvc.perform(delete("/api/admin/courses/" + id).cookie(session).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Không xóa được khóa học còn bài học"));

        assertThat(demKhoa()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lessons", Integer.class)).isEqualTo(1);
    }

    @Test
    void xoaKhoaKhongTonTaiBi404() throws Exception {
        Cookie session = dangNhapAdmin();

        mockMvc.perform(delete("/api/admin/courses/999999").cookie(session).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
