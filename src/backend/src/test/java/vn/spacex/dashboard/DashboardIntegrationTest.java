package vn.spacex.dashboard;

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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng với MySQL 8.4 thật: các câu truy vấn đếm và nhóm chỉ kiểm chứng được trên database thật.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DashboardIntegrationTest {

    private static final String ADMIN_EMAIL = "quan.tri@example.com";
    private static final String ADMIN_MAT_KHAU = "mat-khau-quan-tri-1";
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

    private long adminId;

    // Giữ ADMIN đầu tiên (tạo một lần lúc khởi động); xóa mọi dữ liệu khác theo thứ tự khóa ngoại.
    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lesson_progress");
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users WHERE email <> ?", ADMIN_EMAIL);
        jdbc.update("UPDATE users SET created_at = ? WHERE email = ?", LocalDateTime.now().minusDays(100), ADMIN_EMAIL);
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

    private long dangKy(String email, int ngayTruoc) throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Học sinh","email":"%s","password":"%s","confirmPassword":"%s"}"""
                                .formatted(email, HS_MAT_KHAU, HS_MAT_KHAU)))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE users SET created_at = ? WHERE email = ?", LocalDateTime.now().minusDays(ngayTruoc), email);
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private long themKhoa(String slug, String title, int sortOrder) {
        jdbc.update("INSERT INTO courses (slug, title, sort_order) VALUES (?, ?, ?)", slug, title, sortOrder);
        return jdbc.queryForObject("SELECT id FROM courses WHERE slug = ?", Long.class, slug);
    }

    private long themBai(long courseId, String slug, String status) {
        jdbc.update("INSERT INTO lessons (course_id, created_by, slug, title, content_md, status) "
                + "VALUES (?, ?, ?, ?, 'Nội dung', ?)", courseId, adminId, slug, "Bài " + slug, status);
        return jdbc.queryForObject("SELECT id FROM lessons WHERE slug = ?", Long.class, slug);
    }

    private void themTienDo(long userId, long lessonId, String status) {
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, status) VALUES (?, ?, ?)",
                userId, lessonId, status);
    }

    @Test
    void khachVaHocSinhKhongXemDuocDashboard() throws Exception {
        dangKy("hs@example.com", 1);
        Cookie hs = dangNhap("hs@example.com", HS_MAT_KHAU);

        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/dashboard").cookie(hs)).andExpect(status().isForbidden());
    }

    @Test
    void databaseChiCoAdminThiMoiSoDeuBangKhongTruNguoiDungVaKhongCoKhoa() throws Exception {
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(get("/api/admin/dashboard").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.total").value(1))
                .andExpect(jsonPath("$.users.newLast7Days").value(0))
                .andExpect(jsonPath("$.lessons.total").value(0))
                .andExpect(jsonPath("$.courses", hasSize(0)))
                .andExpect(jsonPath("$.learning.activeLearners").value(0))
                .andExpect(jsonPath("$.learning.completedLessons").value(0));
    }

    @Test
    void demDungNguoiDungBaiTheoKhoaVaTienDoTrenDatabaseThat() throws Exception {
        long moi = dangKy("moi@example.com", 2);
        long cu = dangKy("cu@example.com", 30);
        long chuaHoc = dangKy("chuahoc@example.com", 40);
        long khoa10 = themKhoa("tin-hoc-10", "Tin học 10", 1);
        long khoa11 = themKhoa("tin-hoc-11", "Tin học 11", 2);
        themKhoa("tin-hoc-12", "Tin học 12", 3);
        long b1 = themBai(khoa10, "b1", "PUBLISHED");
        long b2 = themBai(khoa10, "b2", "PUBLISHED");
        themBai(khoa10, "b3", "DRAFT");
        long b4 = themBai(khoa11, "b4", "PUBLISHED");
        themTienDo(moi, b1, "COMPLETED");
        themTienDo(moi, b2, "COMPLETED");
        themTienDo(cu, b1, "COMPLETED");
        themTienDo(cu, b4, "IN_PROGRESS");
        // Quản trị viên đọc thử bài: không được tính vào số học sinh hay lượt hoàn thành.
        themTienDo(adminId, b1, "COMPLETED");
        Cookie admin = dangNhap(ADMIN_EMAIL, ADMIN_MAT_KHAU);

        mockMvc.perform(get("/api/admin/dashboard").cookie(admin))
                .andExpect(status().isOk())
                // admin (100 ngày trước) + 3 học sinh; chỉ "moi" đăng ký trong 7 ngày qua
                .andExpect(jsonPath("$.users.total").value(4))
                .andExpect(jsonPath("$.users.newLast7Days").value(1))
                .andExpect(jsonPath("$.lessons.total").value(4))
                .andExpect(jsonPath("$.lessons.published").value(3))
                .andExpect(jsonPath("$.lessons.draft").value(1))
                .andExpect(jsonPath("$.courses", hasSize(3)))
                .andExpect(jsonPath("$.courses[0].slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$.courses[0].published").value(2))
                .andExpect(jsonPath("$.courses[0].draft").value(1))
                .andExpect(jsonPath("$.courses[1].slug").value("tin-hoc-11"))
                .andExpect(jsonPath("$.courses[1].published").value(1))
                .andExpect(jsonPath("$.courses[1].draft").value(0))
                .andExpect(jsonPath("$.courses[2].slug").value("tin-hoc-12"))
                .andExpect(jsonPath("$.courses[2].published").value(0))
                .andExpect(jsonPath("$.courses[2].draft").value(0))
                // moi và cu có tiến độ; chuaHoc thì không; admin bị loại
                .andExpect(jsonPath("$.learning.activeLearners").value(2))
                .andExpect(jsonPath("$.learning.completedLessons").value(3))
                .andExpect(content().string(not(containsString("@"))));

        assertThat(chuaHoc).isPositive();
    }
}
