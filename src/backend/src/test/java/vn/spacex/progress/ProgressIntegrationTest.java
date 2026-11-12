package vn.spacex.progress;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.user.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dữ liệu thật trong MySQL 8.4 và luật phân quyền thật của SecurityConfig. Hai học sinh được đóng vai bằng user(AppUserDetails)
// mang id thật trong bảng users, đúng như session thật làm.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProgressIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinhDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    private static final String HOAN_THANH = "{\"status\":\"COMPLETED\"}";
    private static final String DANG_HOC = "{\"status\":\"IN_PROGRESS\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long tacGiaId;
    private long khoaId;
    private long hocSinhA;
    private long hocSinhB;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lesson_progress");
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users");
        tacGiaId = themNguoiDung("tacgia@example.com", "ADMIN");
        hocSinhA = themNguoiDung("a@example.com", "LEARNER");
        hocSinhB = themNguoiDung("b@example.com", "LEARNER");
        khoaId = themKhoa("tin-hoc-10", 1);
    }

    private long themNguoiDung(String email, String vaiTro) {
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, enabled)
                VALUES (?, 'khong-dung-de-dang-nhap', ?, ?, 'LOCAL', 1)""", email, email, vaiTro);
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private long themKhoa(String slug, int thuTu) {
        jdbc.update("INSERT INTO courses (slug, title, description, sort_order) VALUES (?, ?, 'mo ta', ?)",
                slug, "Khóa " + slug, thuTu);
        return jdbc.queryForObject("SELECT id FROM courses WHERE slug = ?", Long.class, slug);
    }

    private long themBai(long khoa, String slug, String trangThai, int thuTu) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, 'tom tat', 'noi dung', ?, ?)""",
                khoa, tacGiaId, slug, "Bài " + slug, trangThai, thuTu);
        return jdbc.queryForObject("SELECT id FROM lessons WHERE slug = ?", Long.class, slug);
    }

    private static RequestPostProcessor dangNhap(long id) {
        return user(new AppUserDetails(id, "u" + id + "@example.com", null, "Học sinh " + id, Role.LEARNER, true));
    }

    private void ghi(long nguoiDung, long baiId, String than) throws Exception {
        mockMvc.perform(put("/api/me/progress/lessons/" + baiId).with(dangNhap(nguoiDung)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(than))
                .andExpect(status().isOk());
    }

    private int demDong(long nguoiDung) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM lesson_progress WHERE user_id = ?", Integer.class, nguoiDung);
    }

    // ---- Mỗi người chỉ đổi và xem được tiến độ của chính mình (test bắt buộc của CP5.5) ----

    @Test
    void haiHocSinhCungHocMotBaiThiMoiNguoiCoDongRiengVaKhongThayTienDoCuaNhau() throws Exception {
        long bai = themBai(khoaId, "bai-1", "PUBLISHED", 1);
        themBai(khoaId, "bai-2", "PUBLISHED", 2);

        ghi(hocSinhA, bai, HOAN_THANH);

        assertThat(demDong(hocSinhA)).isEqualTo(1);
        assertThat(demDong(hocSinhB)).isZero();
        mockMvc.perform(get("/api/me/progress").with(dangNhap(hocSinhA)))
                .andExpect(jsonPath("$[0].completedLessons").value(1))
                .andExpect(jsonPath("$[0].percent").value(50));
        mockMvc.perform(get("/api/me/progress").with(dangNhap(hocSinhB)))
                .andExpect(jsonPath("$[0].completedLessons").value(0))
                .andExpect(jsonPath("$[0].percent").value(0))
                .andExpect(jsonPath("$[0].continueLesson.slug").value("bai-1"));

        ghi(hocSinhB, bai, DANG_HOC);

        assertThat(demDong(hocSinhA)).isEqualTo(1);
        assertThat(demDong(hocSinhB)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM lesson_progress WHERE user_id = ?", String.class, hocSinhA))
                .isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT status FROM lesson_progress WHERE user_id = ?", String.class, hocSinhB))
                .isEqualTo("IN_PROGRESS");
    }

    @Test
    void userIdGuiTrongThanHayTrenDuongDanKhongDoiDuocTienDoCuaNguoiKhac() throws Exception {
        long bai = themBai(khoaId, "bai-1", "PUBLISHED", 1);

        mockMvc.perform(put("/api/me/progress/lessons/" + bai).param("userId", String.valueOf(hocSinhB))
                        .with(dangNhap(hocSinhA)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"userId\":" + hocSinhB + "}"))
                .andExpect(status().isOk());

        assertThat(demDong(hocSinhA)).isEqualTo(1);
        assertThat(demDong(hocSinhB)).isZero();
    }

    @Test
    void hocLaiKhoaChiXoaTienDoCuaNguoiGoiVaChiTrongKhoaDo() throws Exception {
        long bai1 = themBai(khoaId, "bai-1", "PUBLISHED", 1);
        long bai2 = themBai(khoaId, "bai-2", "PUBLISHED", 2);
        long khoaKhac = themKhoa("tin-hoc-11", 2);
        long baiKhoaKhac = themBai(khoaKhac, "bai-lop-11", "PUBLISHED", 1);
        ghi(hocSinhA, bai1, HOAN_THANH);
        ghi(hocSinhA, bai2, DANG_HOC);
        ghi(hocSinhA, baiKhoaKhac, HOAN_THANH);
        ghi(hocSinhB, bai1, HOAN_THANH);

        mockMvc.perform(delete("/api/me/progress/courses/" + khoaId).with(dangNhap(hocSinhA)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE user_id = ? AND lesson_id IN (?, ?)",
                Integer.class, hocSinhA, bai1, bai2)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
                Integer.class, hocSinhA, baiKhoaKhac)).isEqualTo(1);
        assertThat(demDong(hocSinhB)).isEqualTo(1);
    }

    @Test
    void hocLaiKhoaChuaTungHocVanTra204VaKhoaKhongTonTaiTra404() throws Exception {
        mockMvc.perform(delete("/api/me/progress/courses/" + khoaId).with(dangNhap(hocSinhA)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/me/progress/courses/999999").with(dangNhap(hocSinhA)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ---- Ghi nhận tiến độ ----

    @Test
    void baiNhapVaBaiKhongTonTaiCungTra404KhongTaoDongTienDo() throws Exception {
        long nhap = themBai(khoaId, "bai-nhap", "DRAFT", 1);

        String chiTietNhap = mockMvc.perform(put("/api/me/progress/lessons/" + nhap).with(dangNhap(hocSinhA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(HOAN_THANH))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String chiTietKhongCo = mockMvc.perform(put("/api/me/progress/lessons/999999").with(dangNhap(hocSinhA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(HOAN_THANH))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // Chỉ khác ở trường instance (đường dẫn người gọi gửi); không dò được id nào là bài nháp.
        assertThat(chiTietNhap.replace(String.valueOf(nhap), "N")).isEqualTo(chiTietKhongCo.replace("999999", "N"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_progress", Integer.class)).isZero();
    }

    @Test
    void ghiLaiNhieuLanChiCoMotDongVaBaiHoanThanhKhongBiHa() throws Exception {
        long bai = themBai(khoaId, "bai-1", "PUBLISHED", 1);

        ghi(hocSinhA, bai, DANG_HOC);
        ghi(hocSinhA, bai, HOAN_THANH);
        var hoanThanhLanDau = jdbc.queryForObject("SELECT completed_at FROM lesson_progress WHERE user_id = ?",
                java.sql.Timestamp.class, hocSinhA);
        ghi(hocSinhA, bai, HOAN_THANH);
        mockMvc.perform(put("/api/me/progress/lessons/" + bai).with(dangNhap(hocSinhA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DANG_HOC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(demDong(hocSinhA)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM lesson_progress WHERE user_id = ?", String.class, hocSinhA))
                .isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT completed_at FROM lesson_progress WHERE user_id = ?",
                java.sql.Timestamp.class, hocSinhA)).isEqualTo(hoanThanhLanDau);
    }

    // Bấm đúp hoặc mở hai tab: nhiều yêu cầu đầu tiên đến cùng lúc cho cùng (học sinh, bài). UNIQUE chặn dòng trùng
    // và không yêu cầu nào được trả 500.
    @Test
    void nhieuYeuCauDauTienDenCungLucDeuThanhCongVaChiCoMotDong() throws Exception {
        long bai = themBai(khoaId, "bai-1", "PUBLISHED", 1);
        int soLuong = 8;
        ExecutorService pool = Executors.newFixedThreadPool(soLuong);
        CountDownLatch sanSang = new CountDownLatch(soLuong);
        CountDownLatch xuatPhat = new CountDownLatch(1);
        try {
            List<Future<Integer>> ketQua = new ArrayList<>();
            for (int i = 0; i < soLuong; i++) {
                Callable<Integer> viec = () -> {
                    sanSang.countDown();
                    xuatPhat.await();
                    return mockMvc.perform(put("/api/me/progress/lessons/" + bai).with(dangNhap(hocSinhA)).with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON).content(DANG_HOC))
                            .andReturn().getResponse().getStatus();
                };
                ketQua.add(pool.submit(viec));
            }
            sanSang.await();
            xuatPhat.countDown();
            for (Future<Integer> f : ketQua) {
                assertThat(f.get()).isEqualTo(200);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(demDong(hocSinhA)).isEqualTo(1);
    }

    // ---- Tiến độ từng khóa trên dữ liệu thật ----

    @Test
    void phanTramVaHocTiepTinhTrenBaiDaDangVaTuTinhLaiKhiAdminAnBai() throws Exception {
        long bai1 = themBai(khoaId, "bai-1", "PUBLISHED", 1);
        long bai2 = themBai(khoaId, "bai-2", "PUBLISHED", 2);
        themBai(khoaId, "bai-3", "PUBLISHED", 3);
        themBai(khoaId, "bai-nhap", "DRAFT", 4);
        themKhoa("tin-hoc-12", 3);

        ghi(hocSinhA, bai1, HOAN_THANH);
        mockMvc.perform(get("/api/me/progress").with(dangNhap(hocSinhA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].courseSlug").value("tin-hoc-10"))
                .andExpect(jsonPath("$[0].completedLessons").value(1))
                .andExpect(jsonPath("$[0].totalLessons").value(3))
                .andExpect(jsonPath("$[0].percent").value(33))
                .andExpect(jsonPath("$[0].continueLesson.slug").value("bai-2"))
                .andExpect(jsonPath("$[1].courseSlug").value("tin-hoc-12"))
                .andExpect(jsonPath("$[1].totalLessons").value(0))
                .andExpect(jsonPath("$[1].percent").value(0))
                .andExpect(jsonPath("$[1].continueLesson").doesNotExist());

        // Bài đang học xem gần nhất được ưu tiên hơn bài đứng trước theo thứ tự.
        ghi(hocSinhA, bai2, DANG_HOC);
        mockMvc.perform(get("/api/me/progress").with(dangNhap(hocSinhA)))
                .andExpect(jsonPath("$[0].continueLesson.slug").value("bai-2"));

        // Admin ẩn bài đã hoàn thành: tổng và phần trăm tự tính lại, không đếm bài đã ẩn.
        jdbc.update("UPDATE lessons SET status = 'DRAFT' WHERE id = ?", bai1);
        mockMvc.perform(get("/api/me/progress").with(dangNhap(hocSinhA)))
                .andExpect(jsonPath("$[0].completedLessons").value(0))
                .andExpect(jsonPath("$[0].totalLessons").value(2))
                .andExpect(jsonPath("$[0].percent").value(0));
    }

    @Test
    void xoaBaiThiTienDoCuaBaiDoMatTheoKhoaNgoaiCascade() throws Exception {
        long bai = themBai(khoaId, "bai-1", "PUBLISHED", 1);
        ghi(hocSinhA, bai, HOAN_THANH);
        ghi(hocSinhB, bai, DANG_HOC);

        jdbc.update("DELETE FROM lessons WHERE id = ?", bai);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_progress", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id IN (?, ?)", Integer.class, hocSinhA, hocSinhB))
                .isEqualTo(2);
    }
}
