package vn.spacex.course;

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
import vn.spacex.support.CourseRows;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Người gọi luôn ẩn danh (không cookie): hai API này công khai. Dữ liệu thật trong MySQL 8.4.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CourseIntegrationTest {

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

    private long tacGiaId;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users");
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, enabled)
                VALUES ('tacgia@example.com', 'khong-dung-de-dang-nhap', 'Tác giả', 'ADMIN', 'LOCAL', 1)""");
        tacGiaId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'tacgia@example.com'", Long.class);
    }

    private long themKhoa(String slug, String mota, int thuTu) {
        return CourseRows.insert(jdbc, slug, "Khóa " + slug, mota, thuTu);
    }

    private void themBai(long khoaId, String slug, String trangThai, int thuTu, String noiDung) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                khoaId, tacGiaId, slug, "Bài " + slug, "Tóm tắt " + slug, noiDung, trangThai, thuTu);
    }

    @Test
    void khoaSapTheoSortOrderRoiDenIdKhiBangNhau() throws Exception {
        themKhoa("c", "khoa c", 2);
        themKhoa("a", "khoa a", 1);
        themKhoa("b", "khoa b", 1);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("a", "b", "c")));
    }

    @Test
    void moiKhoaKemSoBaiDaDangKhongTinhBaiNhapVaKhoaChuaCoBaiLaKhong() throws Exception {
        long coBai = themKhoa("a", "khoa a", 1);
        themKhoa("b", "khoa b", 2);
        themBai(coBai, "a-1", "PUBLISHED", 1, "x");
        themBai(coBai, "a-2", "PUBLISHED", 2, "x");
        themBai(coBai, "a-nhap", "DRAFT", 3, "x");

        mockMvc.perform(get("/api/courses"))
                .andExpect(jsonPath("$[0].lessonCount").value(2))
                .andExpect(jsonPath("$[1].lessonCount").value(0));
    }

    private void ban(long id, String level, int hours, long tuition) {
        jdbc.update("UPDATE courses SET level = ?, total_hours = ?, tuition_vnd = ? WHERE id = ?",
                level, hours, tuition, id);
    }

    private void khoaDeLoc() {
        ban(themKhoa("co-ban", "x", 1), "BASIC", 20, 1_000_000);
        ban(themKhoa("nang-cao", "x", 2), "ADVANCED", 40, 3_000_000);
        ban(themKhoa("chuyen-sau", "x", 3), "EXPERT", 80, 2_000_000);
    }

    @Test
    void chiKhoaDaDangMoiHienTrongDanhSachCongKhai() throws Exception {
        themKhoa("da-dang", "x", 1);
        jdbc.update("UPDATE courses SET status = 'DRAFT' WHERE id = ?", themKhoa("ban-nhap", "x", 2));
        jdbc.update("UPDATE courses SET status = 'ARCHIVED' WHERE id = ?", themKhoa("ngung-ban", "x", 3));

        mockMvc.perform(get("/api/courses"))
                .andExpect(jsonPath("$[*].slug", contains("da-dang")));
        mockMvc.perform(get("/api/courses/ban-nhap/lessons")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/courses/ngung-ban/lessons")).andExpect(status().isNotFound());
    }

    @Test
    void locTheoTrinhDoSoGioVaHocPhiTrenMySqlThat() throws Exception {
        khoaDeLoc();

        mockMvc.perform(get("/api/courses?level=ADVANCED"))
                .andExpect(jsonPath("$[*].slug", contains("nang-cao")));
        mockMvc.perform(get("/api/courses?duration=20-40"))
                .andExpect(jsonPath("$[*].slug", contains("co-ban", "nang-cao")));
        mockMvc.perform(get("/api/courses?duration=41-"))
                .andExpect(jsonPath("$[*].slug", contains("chuyen-sau")));
        // Số vượt kiểu cột không gây lỗi.
        mockMvc.perform(get("/api/courses?duration=-9999999999"))
                .andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(get("/api/courses?price=2000000-"))
                .andExpect(jsonPath("$[*].slug", contains("nang-cao", "chuyen-sau")));
        mockMvc.perform(get("/api/courses?level=BASIC&price=2000000-"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sapTheoHocPhiTangVaGiam() throws Exception {
        khoaDeLoc();

        mockMvc.perform(get("/api/courses?sort=price_asc"))
                .andExpect(jsonPath("$[*].slug", contains("co-ban", "chuyen-sau", "nang-cao")));
        mockMvc.perform(get("/api/courses?sort=price_desc"))
                .andExpect(jsonPath("$[*].slug", contains("nang-cao", "chuyen-sau", "co-ban")));
    }

    @Test
    void locTheoNhomVaDanhSachNhomLaCongKhai() throws Exception {
        jdbc.update("DELETE FROM categories WHERE slug = 'nhom-thu'");
        jdbc.update("INSERT INTO categories (slug, name, sort_order) VALUES ('nhom-thu', 'Nhóm thử', -1)");
        themKhoa("o-nhom-mac-dinh", "x", 1);
        jdbc.update("UPDATE courses SET category_id = (SELECT id FROM categories WHERE slug = 'nhom-thu') "
                + "WHERE id = ?", themKhoa("o-nhom-thu", "x", 2));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("nhom-thu", "khoa-cho-thieu-nien")))
                .andExpect(jsonPath("$[0].name").value("Nhóm thử"))
                .andExpect(jsonPath("$[0].*", hasSize(3)));
        mockMvc.perform(get("/api/courses?category=nhom-thu"))
                .andExpect(jsonPath("$[*].slug", contains("o-nhom-thu")))
                .andExpect(jsonPath("$[0].category.name").value("Nhóm thử"));
        mockMvc.perform(get("/api/courses?category=khong-co"))
                .andExpect(jsonPath("$", hasSize(0)));

        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM categories WHERE slug = 'nhom-thu'");
    }

    @Test
    void moiKhoaCoMuoiTruongVaMoTaVaAnhBiaDeTrongThiLaNull() throws Exception {
        long id = themKhoa("tin-hoc-10", null, 1);

        mockMvc.perform(get("/api/courses"))
                .andExpect(jsonPath("$[0].*", hasSize(10)))
                .andExpect(jsonPath("$[0].coverImageUrl").value(nullValue()))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$[0].title").value("Khóa tin-hoc-10"))
                .andExpect(jsonPath("$[0].description").value(nullValue()));
    }

    @Test
    void baiChiLayBaiDaDangVaKhongLoNoiDungNhuLanBaiNhap() throws Exception {
        long khoa = themKhoa("tin-hoc-10", "mo ta", 1);
        themBai(khoa, "bai-da-dang", "PUBLISHED", 1, "NOI-DUNG-BAI-GIANG-BI-MAT");
        themBai(khoa, "bai-nhap", "DRAFT", 2, "BAN-NHAP-CHUA-DANG");

        MvcResult result = mockMvc.perform(get("/api/courses/tin-hoc-10/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value("bai-da-dang"))
                .andExpect(jsonPath("$[0].*", hasSize(5)))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).doesNotContain("NOI-DUNG-BAI-GIANG-BI-MAT")
                .doesNotContain("BAN-NHAP-CHUA-DANG")
                .doesNotContain("bai-nhap");
    }

    @Test
    void baiSapTheoSortOrderRoiDenId() throws Exception {
        long khoa = themKhoa("tin-hoc-10", "mo ta", 1);
        themBai(khoa, "z-sau", "PUBLISHED", 3, "x");
        themBai(khoa, "m-giua-truoc", "PUBLISHED", 2, "x");
        themBai(khoa, "n-giua-sau", "PUBLISHED", 2, "x");
        themBai(khoa, "a-dau", "PUBLISHED", 1, "x");

        mockMvc.perform(get("/api/courses/tin-hoc-10/lessons"))
                .andExpect(jsonPath("$[*].slug", contains("a-dau", "m-giua-truoc", "n-giua-sau", "z-sau")));
    }

    @Test
    void baiCuaKhoaKhacKhongLanVaoDanhSach() throws Exception {
        long khoa10 = themKhoa("tin-hoc-10", "mo ta", 1);
        long khoa11 = themKhoa("tin-hoc-11", "mo ta", 2);
        themBai(khoa10, "bai-lop-10", "PUBLISHED", 1, "x");
        themBai(khoa11, "bai-lop-11", "PUBLISHED", 1, "x");

        mockMvc.perform(get("/api/courses/tin-hoc-11/lessons"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value("bai-lop-11"));
    }

    @Test
    void khoaChuaCoBaiHoacChiCoBaiNhapTraVeMangRongVaKhongPhaiLoi() throws Exception {
        long khoaRong = themKhoa("rong", "mo ta", 1);
        long khoaChiNhap = themKhoa("chi-nhap", "mo ta", 2);
        themBai(khoaChiNhap, "mot-ban-nhap", "DRAFT", 1, "x");

        mockMvc.perform(get("/api/courses/rong/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/courses/chi-nhap/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        assertThat(khoaRong).isNotEqualTo(khoaChiNhap);
    }

    @Test
    void khoaKhongTonTaiBi404DangProblemJsonVaThongBaoKhongLapLaiSlug() throws Exception {
        // detail là câu cố định. Trường instance (chuẩn RFC 9457) là đường dẫn của chính yêu cầu nên có slug, không kiểm.
        mockMvc.perform(get("/api/courses/khong-co-khoa-nay/lessons"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));
    }

    @Test
    void tenKhoaVaBaiTiengVietCoDauDiQuaMySqlVaJsonKhongBiHong() throws Exception {
        long khoa = CourseRows.insert(jdbc, "tin-hoc-12", "Tin học 12 — Hệ cơ sở dữ liệu",
                "Ứng dụng tin học cho học sinh THPT", 1);
        themBai(khoa, "mo-dau", "PUBLISHED", 1, "x");

        mockMvc.perform(get("/api/courses"))
                .andExpect(jsonPath("$[0].title").value("Tin học 12 — Hệ cơ sở dữ liệu"))
                .andExpect(jsonPath("$[0].description").value("Ứng dụng tin học cho học sinh THPT"));
        mockMvc.perform(get("/api/courses/tin-hoc-12/lessons"))
                .andExpect(jsonPath("$[0].summary").value("Tóm tắt mo-dau"));
    }
}
