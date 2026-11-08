package vn.spacex.lesson;

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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng với MySQL 8.4 thật: ADMIN tạo lúc khởi động, đăng nhập bằng session thật rồi gọi API Admin.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminLessonIntegrationTest {

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
    private long khoa10;
    private long khoa11;

    // Không xóa ADMIN: nó chỉ được tạo một lần lúc khởi động.
    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM knowledge_chunks");
        jdbc.update("DELETE FROM lesson_images");
        jdbc.update("DELETE FROM lesson_progress");
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users WHERE role = 'LEARNER'");
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE role = 'ADMIN'", Long.class);
        khoa10 = themKhoa("tin-hoc-10", 1);
        khoa11 = themKhoa("tin-hoc-11", 2);
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

    private long themKhoa(String slug, int thuTu) {
        jdbc.update("INSERT INTO courses (slug, title, description, sort_order) VALUES (?, ?, NULL, ?)",
                slug, "Khóa " + slug, thuTu);
        return jdbc.queryForObject("SELECT id FROM courses WHERE slug = ?", Long.class, slug);
    }

    private long themBai(long khoaId, String slug, String trangThai, int thuTu, String noiDung) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                khoaId, adminId, slug, "Bài " + slug, "Tóm tắt " + slug, noiDung, trangThai, thuTu);
        return jdbc.queryForObject("SELECT id FROM lessons WHERE slug = ?", Long.class, slug);
    }

    private static String body(long khoaId, String slug, String title, String contentMd, int thuTu) {
        return """
                {"courseId":%d,"slug":"%s","title":"%s","summary":"Tóm tắt","contentMd":"%s","sortOrder":%d}"""
                .formatted(khoaId, slug, title, contentMd, thuTu);
    }

    private int demBai() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM lessons", Integer.class);
    }

    // ---- Phân quyền ----

    @Test
    void khachVaHocSinhKhongDocDuocBaiNhapQuaApiAdmin() throws Exception {
        long nhap = themBai(khoa10, "bai-nhap", "DRAFT", 1, "NOI-DUNG-NHAP-BI-MAT");
        Cookie hs = dangNhapHocSinh();

        mockMvc.perform(get("/api/admin/lessons")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/lessons/" + nhap)).andExpect(status().isUnauthorized());
        MvcResult r = mockMvc.perform(get("/api/admin/lessons/" + nhap).cookie(hs))
                .andExpect(status().isForbidden()).andReturn();
        mockMvc.perform(get("/api/admin/lessons").cookie(hs)).andExpect(status().isForbidden());

        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("NOI-DUNG-NHAP-BI-MAT");
    }

    @Test
    void hocSinhTaoSuaDoiTrangThaiXoaDeuBi403VaDatabaseKhongDoi() throws Exception {
        long bai = themBai(khoa10, "co-san", "DRAFT", 1, "x");
        Cookie hs = dangNhapHocSinh();

        mockMvc.perform(post("/api/admin/lessons").cookie(hs).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body(khoa10, "moi", "Mới", "n", 1))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/lessons/" + bai).cookie(hs).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body(khoa10, "da-sua", "Sửa", "n", 1))).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/lessons/" + bai + "/status").cookie(hs).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/lessons/" + bai).cookie(hs).with(csrf())).andExpect(status().isForbidden());

        assertThat(demBai()).isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap("SELECT slug, status FROM lessons WHERE id = ?", bai);
        assertThat(row.get("slug")).isEqualTo("co-san");
        assertThat(row.get("status")).isEqualTo("DRAFT");
    }

    @Test
    void adminDaDangNhapNhungThieuCsrfTokenBi403() throws Exception {
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/lessons").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content(body(khoa10, "moi", "Mới", "n", 1))).andExpect(status().isForbidden());

        assertThat(demBai()).isZero();
    }

    // ---- Tạo và vòng đời nháp → đăng → ẩn ----

    @Test
    void taoBaiLaNhapNguoiTaoLaAdminDangNhapNoiDungNguyenVenVaHocSinhChuaThayDuoc() throws Exception {
        Cookie admin = dangNhapAdmin();
        String markdown = "# Máy tính\\n\\nĐoạn **đậm** và `code`.\\n\\n- mục một\\n- mục hai";

        mockMvc.perform(post("/api/admin/lessons").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(khoa10, "may-tinh-la-gi", "Máy tính là gì", markdown, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.courseId").value(khoa10));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_by, status, content_md, title FROM lessons WHERE slug = 'may-tinh-la-gi'");
        assertThat(((Number) row.get("created_by")).longValue()).isEqualTo(adminId);
        assertThat(row.get("status")).isEqualTo("DRAFT");
        assertThat(row.get("title")).isEqualTo("Máy tính là gì");
        assertThat(row.get("content_md")).isEqualTo(
                "# Máy tính\n\nĐoạn **đậm** và `code`.\n\n- mục một\n- mục hai");

        mockMvc.perform(get("/api/courses/tin-hoc-10/lessons")).andExpect(jsonPath("$", hasSize(0)));
        Cookie hs = dangNhapHocSinh();
        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").cookie(hs)).andExpect(status().isNotFound());
    }

    @Test
    void dangBaiThiHocSinhDocDuocAnBaiThiBienMat() throws Exception {
        long bai = themBai(khoa10, "bai-1", "DRAFT", 1, "Nội dung bài một");
        Cookie admin = dangNhapAdmin();
        Cookie hs = dangNhapHocSinh();

        mockMvc.perform(patch("/api/admin/lessons/" + bai + "/status").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/courses/tin-hoc-10/lessons"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value("bai-1"));
        mockMvc.perform(get("/api/lessons/bai-1").cookie(hs))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentMd").value("Nội dung bài một"));

        mockMvc.perform(patch("/api/admin/lessons/" + bai + "/status").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/courses/tin-hoc-10/lessons")).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/lessons/bai-1").cookie(hs)).andExpect(status().isNotFound());
    }

    @Test
    void taoBaiVaoKhoaKhongTonTaiBi404VaKhongGhiGi() throws Exception {
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/lessons").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(999999, "moi", "Mới", "n", 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));

        assertThat(demBai()).isZero();
    }

    @Test
    void taoBaiTrungSlugBi409KeCaKhiKhacKhoaVaChiCoMotDong() throws Exception {
        themBai(khoa10, "bai-1", "DRAFT", 1, "x");
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/lessons").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(khoa11, "bai-1", "Khác khóa", "n", 1)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Slug bài học đã tồn tại"));

        assertThat(demBai()).isEqualTo(1);
    }

    @Test
    void taoBaiDuLieuSaiBi400VaKhongGhiGi() throws Exception {
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(post("/api/admin/lessons").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(khoa10, "Bai Mot", "", "n", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)));

        assertThat(demBai()).isZero();
    }

    // ---- Chi tiết và danh sách ----

    @Test
    void adminXemDuocChiTietBaiNhapKemNoiDung() throws Exception {
        long bai = themBai(khoa10, "bai-nhap", "DRAFT", 7, "Nội dung bản nháp");
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(get("/api/admin/lessons/" + bai).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.contentMd").value("Nội dung bản nháp"))
                .andExpect(jsonPath("$.sortOrder").value(7));
        mockMvc.perform(get("/api/admin/lessons/999999").cookie(admin)).andExpect(status().isNotFound());
    }

    @Test
    void danhSachLocTheoKhoaVaTrangThaiPhanTrangVaKhongLoNoiDung() throws Exception {
        themBai(khoa10, "a-10-dau", "PUBLISHED", 1, "NOI-DUNG-BI-MAT");
        themBai(khoa10, "b-10-nhap", "DRAFT", 2, "x");
        themBai(khoa10, "c-10-cuoi", "PUBLISHED", 3, "x");
        themBai(khoa11, "d-11", "PUBLISHED", 1, "x");
        themBai(khoa11, "e-11-nhap", "DRAFT", 2, "x");
        Cookie admin = dangNhapAdmin();

        MvcResult tatCa = mockMvc.perform(get("/api/admin/lessons").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[*].slug", contains("a-10-dau", "b-10-nhap", "c-10-cuoi", "d-11", "e-11-nhap")))
                .andExpect(jsonPath("$.items[0].*", hasSize(7)))
                .andReturn();
        assertThat(tatCa.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("NOI-DUNG-BI-MAT");

        mockMvc.perform(get("/api/admin/lessons?courseId=" + khoa11).cookie(admin))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].slug", contains("d-11", "e-11-nhap")));
        mockMvc.perform(get("/api/admin/lessons?status=DRAFT").cookie(admin))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].slug", contains("b-10-nhap", "e-11-nhap")));
        mockMvc.perform(get("/api/admin/lessons?courseId=" + khoa10 + "&status=PUBLISHED").cookie(admin))
                .andExpect(jsonPath("$.items[*].slug", contains("a-10-dau", "c-10-cuoi")));

        mockMvc.perform(get("/api/admin/lessons?page=1&size=2").cookie(admin))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.items[*].slug", contains("c-10-cuoi", "d-11")));
        mockMvc.perform(get("/api/admin/lessons?page=5&size=2").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(5));
    }

    @Test
    void kichThuocTrangQua50Bi400() throws Exception {
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(get("/api/admin/lessons?size=51").cookie(admin)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/lessons?size=50").cookie(admin)).andExpect(status().isOk());
    }

    // ---- Sửa ----

    @Test
    void suaBaiGhiDungVaGiuNguyenTrangThaiVaNguoiTaoKeCaKhiChuyenKhoa() throws Exception {
        long bai = themBai(khoa10, "bai-1", "PUBLISHED", 1, "cũ");
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/lessons/" + bai).cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(khoa11, "bai-1-moi", "Tiêu đề mới", "Nội dung mới — có dấu", 9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(khoa11))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT course_id, created_by, slug, title, content_md, status, sort_order FROM lessons WHERE id = ?", bai);
        assertThat(((Number) row.get("course_id")).longValue()).isEqualTo(khoa11);
        assertThat(((Number) row.get("created_by")).longValue()).isEqualTo(adminId);
        assertThat(row.get("slug")).isEqualTo("bai-1-moi");
        assertThat(row.get("title")).isEqualTo("Tiêu đề mới");
        assertThat(row.get("content_md")).isEqualTo("Nội dung mới — có dấu");
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        assertThat(row.get("sort_order")).isEqualTo(9);
    }

    @Test
    void suaBaiGiuNguyenSlugCuaChinhNoKhongBiBaoTrung() throws Exception {
        long bai = themBai(khoa10, "bai-1", "DRAFT", 1, "x");
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/lessons/" + bai).cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(khoa10, "bai-1", "Đổi tiêu đề", "x", 1)))
                .andExpect(status().isOk());
    }

    @Test
    void suaBaiDoiSangSlugCuaBaiKhacBi409VaCaHaiBaiGiuNguyen() throws Exception {
        long bai1 = themBai(khoa10, "bai-1", "DRAFT", 1, "x");
        themBai(khoa10, "bai-2", "DRAFT", 2, "x");
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/lessons/" + bai1).cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(khoa10, "bai-2", "Đổi", "x", 1)))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT slug FROM lessons WHERE id = ?", String.class, bai1)).isEqualTo("bai-1");
    }

    @Test
    void suaBaiChuyenSangKhoaKhongTonTaiBi404VaBaiGiuNguyen() throws Exception {
        long bai = themBai(khoa10, "bai-1", "DRAFT", 1, "x");
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/lessons/" + bai).cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(999999, "bai-1", "Đổi", "x", 1)))
                .andExpect(status().isNotFound());

        assertThat(((Number) jdbc.queryForObject("SELECT course_id FROM lessons WHERE id = ?", Long.class, bai))
                .longValue()).isEqualTo(khoa10);
    }

    @Test
    void suaVaDoiTrangThaiBaiKhongTonTaiBi404() throws Exception {
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(put("/api/admin/lessons/999999").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(khoa10, "x", "X", "n", 1)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/lessons/999999/status").cookie(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- Xóa ----

    @Test
    void xoaBaiXoaLuonAnhTienDoVaDoanTriThucNhungGiuNguoiDungVaKhoa() throws Exception {
        long bai = themBai(khoa10, "bai-1", "PUBLISHED", 1, "x");
        dangNhapHocSinh();
        long hsId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'hs@example.com'", Long.class);
        jdbc.update("INSERT INTO lesson_images (lesson_id, file_name, content_type, size_bytes, data) VALUES (?, 'a.png', 'image/png', 3, ?)",
                bai, new byte[]{1, 2, 3});
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, status) VALUES (?, ?, 'IN_PROGRESS')", hsId, bai);
        jdbc.update("INSERT INTO knowledge_chunks (lesson_id, chunk_index, heading, content) VALUES (?, 0, 'Mở đầu', 'nội dung')", bai);
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(delete("/api/admin/lessons/" + bai).cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(demBai()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_images", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_progress", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_chunks", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, hsId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courses", Integer.class)).isEqualTo(2);
    }

    @Test
    void xoaBaiKhongTonTaiBi404() throws Exception {
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(delete("/api/admin/lessons/999999").cookie(admin).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void xoaKhoaChiDuocSauKhiXoaHetBaiCuaNo() throws Exception {
        long bai = themBai(khoa10, "bai-1", "DRAFT", 1, "x");
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(delete("/api/admin/courses/" + khoa10).cookie(admin).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/admin/lessons/" + bai).cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/courses/" + khoa10).cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());
    }
}
