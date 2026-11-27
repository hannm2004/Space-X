package vn.spacex.access;

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
import vn.spacex.support.CourseRows;
import vn.spacex.user.Role;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Bảng "Ai thấy gì" của D40 mục 2, kiểm qua HTTP trên MySQL 8.4 thật. Một quy tắc dùng chung cho đọc bài, ảnh bài,
// tìm bài và tiến độ, nên mỗi vai được kiểm ở cả bốn nơi.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LessonAccessIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinhDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    private static final String HOAN_THANH = "{\"status\":\"COMPLETED\"}";

    // Tên khóa = loại + trạng thái. Mỗi khóa có một bài đã đăng "<khóa>-bai" và một bài nháp "<khóa>-nhap".
    private static final List<String> MOI_KHOA = List.of(
            "free-published", "paid-published", "free-archived", "paid-archived", "free-draft", "paid-draft");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final Map<String, Long> khoa = new HashMap<>();
    private final Map<String, Long> bai = new HashMap<>();
    private final Map<String, Long> anh = new HashMap<>();

    private long admin;
    // Chưa ghi danh khóa nào.
    private long nguoiMoi;
    // Ghi danh ACTIVE ở mọi khóa, kể cả khóa nháp.
    private long daGhiDanh;
    // Từng ghi danh mọi khóa nhưng đã bị thu hồi (hoàn tiền).
    private long biThuHoi;

    @BeforeEach
    void dungDuLieu() {
        jdbc.update("DELETE FROM enrollments");
        jdbc.update("DELETE FROM lesson_progress");
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users");
        admin = themNguoiDung("quantri@example.com", "ADMIN");
        nguoiMoi = themNguoiDung("moi@example.com", "LEARNER");
        daGhiDanh = themNguoiDung("ghidanh@example.com", "LEARNER");
        biThuHoi = themNguoiDung("thuhoi@example.com", "LEARNER");
        int thuTu = 0;
        for (String ten : MOI_KHOA) {
            String[] phan = ten.split("-");
            long id = CourseRows.insert(jdbc, ten, "Khóa " + ten, null, ++thuTu);
            jdbc.update("UPDATE courses SET access_type = ?, status = ? WHERE id = ?",
                    phan[0].toUpperCase(), phan[1].toUpperCase(), id);
            khoa.put(ten, id);
            themBai(ten, ten + "-bai", "PUBLISHED", 1);
            themBai(ten, ten + "-nhap", "DRAFT", 2);
            ghiDanh(daGhiDanh, id, "ACTIVE");
            ghiDanh(biThuHoi, id, "REVOKED");
        }
    }

    private long themNguoiDung(String email, String vaiTro) {
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, enabled)
                VALUES (?, 'khong-dung-de-dang-nhap', ?, ?, 'LOCAL', 1)""", email, email, vaiTro);
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    // Mọi bài chứa cùng một từ để tìm kiếm trả về đúng tập bài người gọi được đọc.
    private void themBai(String tenKhoa, String slug, String trangThai, int thuTu) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, 'tom tat', 'thuattoan NOI-DUNG-RIENG', ?, ?)""",
                khoa.get(tenKhoa), admin, slug, "Bài " + slug, trangThai, thuTu);
        long id = jdbc.queryForObject("SELECT id FROM lessons WHERE slug = ?", Long.class, slug);
        bai.put(slug, id);
        jdbc.update("""
                INSERT INTO lesson_images (lesson_id, file_name, content_type, size_bytes, data)
                VALUES (?, 'a.png', 'image/png', 3, X'010203')""", id);
        anh.put(slug, jdbc.queryForObject("SELECT id FROM lesson_images WHERE lesson_id = ?", Long.class, id));
    }

    private void ghiDanh(long nguoiDung, long khoaId, String trangThai) {
        jdbc.update("INSERT INTO enrollments (user_id, course_id, status, source) VALUES (?, ?, ?, 'ORDER')",
                nguoiDung, khoaId, trangThai);
    }

    private static RequestPostProcessor hocSinh(long id) {
        return user(new AppUserDetails(id, "u" + id + "@example.com", null, "Học sinh", Role.LEARNER, true));
    }

    private RequestPostProcessor quanTri() {
        return user(new AppUserDetails(admin, "quantri@example.com", null, "Quản trị", Role.ADMIN, true));
    }

    // Đọc bài, xem ảnh và ghi tiến độ của một bài phải cùng cho một kết quả.
    private void kiemQuyen(RequestPostProcessor nguoiGoi, String slug, boolean duocDoc) throws Exception {
        int mongDoi = duocDoc ? 200 : 404;
        var doc = mockMvc.perform(get("/api/lessons/" + slug).with(nguoiGoi)).andReturn().getResponse();
        assertThat(doc.getStatus()).as("đọc bài %s", slug).isEqualTo(mongDoi);
        if (!duocDoc) {
            assertThat(doc.getContentAsString(StandardCharsets.UTF_8))
                    .as("không lộ nội dung bài %s", slug).doesNotContain("NOI-DUNG-RIENG");
        }
        assertThat(mockMvc.perform(get("/api/images/" + anh.get(slug)).with(nguoiGoi))
                .andReturn().getResponse().getStatus()).as("ảnh của bài %s", slug).isEqualTo(mongDoi);
        assertThat(mockMvc.perform(put("/api/me/progress/lessons/" + bai.get(slug)).with(nguoiGoi).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(HOAN_THANH))
                .andReturn().getResponse().getStatus()).as("ghi tiến độ bài %s", slug).isEqualTo(mongDoi);
    }

    @Test
    void nguoiDaDangNhapChuaGhiDanhChiDocDuocKhoaMienPhiDaDang() throws Exception {
        kiemQuyen(hocSinh(nguoiMoi), "free-published-bai", true);
        kiemQuyen(hocSinh(nguoiMoi), "paid-published-bai", false);
        kiemQuyen(hocSinh(nguoiMoi), "free-archived-bai", false);
        kiemQuyen(hocSinh(nguoiMoi), "paid-archived-bai", false);
    }

    @Test
    void baiCuaKhoaNhapKhongAiNgoaiAdminDocDuocKeCaKhiDaGhiDanh() throws Exception {
        for (RequestPostProcessor nguoiGoi : List.of(hocSinh(nguoiMoi), hocSinh(daGhiDanh))) {
            kiemQuyen(nguoiGoi, "free-draft-bai", false);
            kiemQuyen(nguoiGoi, "paid-draft-bai", false);
        }
    }

    @Test
    void nguoiDaGhiDanhDocDuocKhoaTraPhiVaKhoaDaLuuTru() throws Exception {
        for (String ten : List.of("free-published", "paid-published", "free-archived", "paid-archived")) {
            kiemQuyen(hocSinh(daGhiDanh), ten + "-bai", true);
        }
    }

    @Test
    void ghiDanhBiThuHoiKhongConDocDuocKhoaTraPhiNhungKhoaMienPhiVanMo() throws Exception {
        kiemQuyen(hocSinh(biThuHoi), "free-published-bai", true);
        kiemQuyen(hocSinh(biThuHoi), "paid-published-bai", false);
        kiemQuyen(hocSinh(biThuHoi), "paid-archived-bai", false);
    }

    @Test
    void baiNhapKhongHocSinhNaoDocDuocDuKhoaMoCho() throws Exception {
        for (String ten : MOI_KHOA) {
            kiemQuyen(hocSinh(daGhiDanh), ten + "-nhap", false);
        }
    }

    @Test
    void adminDocDuocMoiBaiKeCaKhoaNhapVaBaiNhap() throws Exception {
        for (String ten : MOI_KHOA) {
            kiemQuyen(quanTri(), ten + "-bai", true);
            kiemQuyen(quanTri(), ten + "-nhap", true);
        }
    }

    @Test
    void baiKhongDuocPhepVaBaiKhongTonTaiTraCungMotCauNenKhongDoDuoc() throws Exception {
        String traPhi = mockMvc.perform(get("/api/lessons/paid-published-bai").with(hocSinh(nguoiMoi)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String khongCo = mockMvc.perform(get("/api/lessons/khong-co-bai-nay").with(hocSinh(nguoiMoi)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(traPhi.replace("paid-published-bai", "SLUG")).isEqualTo(khongCo.replace("khong-co-bai-nay", "SLUG"));
    }

    @Test
    void khachChuaDangNhapKhongDocDuocBaiAnhTimKiemVaTienDo() throws Exception {
        mockMvc.perform(get("/api/lessons/free-published-bai")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/images/" + anh.get("free-published-bai"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/lessons?q=thuattoan")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/progress")).andExpect(status().isUnauthorized());
    }

    // Đề cương (tên bài) của khóa đã đăng là công khai, kể cả khóa trả phí.
    @Test
    void khachVanXemDuocDeCuongKhoaTraPhiDaDang() throws Exception {
        mockMvc.perform(get("/api/courses/paid-published/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", containsInAnyOrder("paid-published-bai")))
                .andExpect(jsonPath("$[0].contentMd").doesNotExist());
        mockMvc.perform(get("/api/courses/paid-draft/lessons")).andExpect(status().isNotFound());
    }

    @Test
    void timKiemChiTraVaChiDemBaiNguoiGoiDuocDoc() throws Exception {
        mockMvc.perform(get("/api/lessons?q=thuattoan").with(hocSinh(nguoiMoi)))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[*].slug", containsInAnyOrder("free-published-bai")));
        mockMvc.perform(get("/api/lessons?q=thuattoan").with(hocSinh(daGhiDanh)))
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.items[*].slug", containsInAnyOrder("free-published-bai",
                        "paid-published-bai", "free-archived-bai", "paid-archived-bai")));
        mockMvc.perform(get("/api/lessons?q=thuattoan").with(hocSinh(biThuHoi)))
                .andExpect(jsonPath("$.totalItems").value(1));
        mockMvc.perform(get("/api/lessons?q=thuattoan").with(quanTri()))
                .andExpect(jsonPath("$.totalItems").value(12));
    }

    @Test
    void tienDoChiLietKeKhoaNguoiGoiDuocHoc() throws Exception {
        mockMvc.perform(get("/api/me/progress").with(hocSinh(nguoiMoi)))
                .andExpect(jsonPath("$[*].courseSlug", containsInAnyOrder("free-published")));
        mockMvc.perform(get("/api/me/progress").with(hocSinh(daGhiDanh)))
                .andExpect(jsonPath("$[*].courseSlug", containsInAnyOrder("free-published", "paid-published",
                        "free-archived", "paid-archived")));
        mockMvc.perform(get("/api/me/progress").with(quanTri()))
                .andExpect(jsonPath("$[*].courseSlug", containsInAnyOrder(MOI_KHOA.toArray())));
    }

    // Chỉ bài đã đăng được tính vào phần trăm, kể cả khi người xem là Admin.
    @Test
    void tienDoCuaAdminCungChiDemBaiDaDang() throws Exception {
        mockMvc.perform(get("/api/me/progress").with(quanTri()))
                .andExpect(jsonPath("$[0].totalLessons").value(1));
    }

    @Test
    void hocLaiChiLamDuocVoiKhoaDangDuocHocVaKhongXoaTienDoKhiBiThuHoi() throws Exception {
        long khoaTraPhi = khoa.get("paid-published");
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id) VALUES (?, ?)",
                biThuHoi, bai.get("paid-published-bai"));

        mockMvc.perform(delete("/api/me/progress/courses/" + khoaTraPhi).with(hocSinh(biThuHoi)).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/me/progress/courses/" + khoa.get("free-draft"))
                        .with(hocSinh(daGhiDanh)).with(csrf()))
                .andExpect(status().isNotFound());

        // D40: thu hồi quyền học nhưng tiến độ vẫn giữ.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_progress WHERE user_id = ?",
                Integer.class, biThuHoi)).isEqualTo(1);

        mockMvc.perform(delete("/api/me/progress/courses/" + khoaTraPhi).with(hocSinh(daGhiDanh)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminKhongXoaDuocKhoaDaCoHocVienGhiDanh() throws Exception {
        jdbc.update("DELETE FROM lessons WHERE course_id = ?", khoa.get("paid-draft"));

        mockMvc.perform(delete("/api/admin/courses/" + khoa.get("paid-draft")).with(quanTri()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Không xóa được khóa học đã có học viên ghi danh"));
    }
}
