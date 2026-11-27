package vn.spacex.lesson;

import vn.spacex.user.Role;
import vn.spacex.auth.AppUserDetails;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dữ liệu thật trong MySQL 8.4; luật phân quyền thật của SecurityConfig. Người "đã đăng nhập" được đóng vai bằng user().
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LessonIntegrationTest {

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
    private long khoaId;

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, "hs@example.com", null, "Học sinh", Role.LEARNER, true);
    }

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users");
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, enabled)
                VALUES ('tacgia@example.com', 'khong-dung-de-dang-nhap', 'Tác giả', 'ADMIN', 'LOCAL', 1)""");
        tacGiaId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'tacgia@example.com'", Long.class);
        khoaId = CourseRows.insert(jdbc, "tin-hoc-10", "Tin học 10", "mo ta", 1);
    }

    private void themBai(String slug, String trangThai, String noiDung) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)""",
                khoaId, tacGiaId, slug, "Bài " + slug, "Tóm tắt " + slug, noiDung, trangThai);
    }

    private long themBaiThuTu(long maKhoa, String slug, String trangThai, int thuTu) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                maKhoa, tacGiaId, slug, "Bài " + slug, "Tóm tắt " + slug, "x", trangThai, thuTu);
        return jdbc.queryForObject("SELECT id FROM lessons WHERE slug = ?", Long.class, slug);
    }

    @Test
    void baiTraKemKhoaVaBaiTruocBaiSauTheoSortOrderBoQuaBaiNhapVaBaiKhoaKhac() throws Exception {
        themBaiThuTu(khoaId, "bai-1", "PUBLISHED", 1);
        themBaiThuTu(khoaId, "bai-nhap", "DRAFT", 2);
        themBaiThuTu(khoaId, "bai-2", "PUBLISHED", 3);
        themBaiThuTu(khoaId, "bai-3", "PUBLISHED", 4);
        long khoaKhac = CourseRows.insert(jdbc, "tin-hoc-11", "Tin học 11", "x", 2);
        themBaiThuTu(khoaKhac, "bai-lop-11", "PUBLISHED", 3);

        mockMvc.perform(get("/api/lessons/bai-2").with(user(hocSinh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$.course.title").value("Tin học 10"))
                // Bài nháp nằm giữa bài 1 và bài 2 nhưng không được tính là "bài trước".
                .andExpect(jsonPath("$.previous.slug").value("bai-1"))
                .andExpect(jsonPath("$.previous.title").value("Bài bai-1"))
                .andExpect(jsonPath("$.next.slug").value("bai-3"));
    }

    @Test
    void baiDauVaBaiCuoiKhoaCoTruongTruocHoacSauLaNull() throws Exception {
        themBaiThuTu(khoaId, "bai-1", "PUBLISHED", 1);
        themBaiThuTu(khoaId, "bai-2", "PUBLISHED", 2);

        mockMvc.perform(get("/api/lessons/bai-1").with(user(hocSinh())))
                .andExpect(jsonPath("$.previous").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.next.slug").value("bai-2"));
        mockMvc.perform(get("/api/lessons/bai-2").with(user(hocSinh())))
                .andExpect(jsonPath("$.previous.slug").value("bai-1"))
                .andExpect(jsonPath("$.next").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void baiBangSortOrderThiXepTheoIdNhuTrangKhoaHoc() throws Exception {
        themBaiThuTu(khoaId, "bai-a", "PUBLISHED", 1);
        themBaiThuTu(khoaId, "bai-b", "PUBLISHED", 1);

        mockMvc.perform(get("/api/lessons/bai-a").with(user(hocSinh())))
                .andExpect(jsonPath("$.next.slug").value("bai-b"));
    }

    @Test
    void baiDaDangTraNoiDungMarkdownNguyenVanVaTiengVietCoDauKhongBiHong() throws Exception {
        String markdown = "# Máy tính là gì?\n\nMáy tính xử lý **thông tin** theo chương trình.\n\n- Phần cứng\n- Phần mềm";
        themBai("may-tinh-la-gi", "PUBLISHED", markdown);

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user(hocSinh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("may-tinh-la-gi"))
                .andExpect(jsonPath("$.title").value("Bài may-tinh-la-gi"))
                .andExpect(jsonPath("$.summary").value("Tóm tắt may-tinh-la-gi"))
                .andExpect(jsonPath("$.contentMd").value(markdown));
    }

    @Test
    void phanHoiChiCoNamTruongKhongLoTrangThaiMaKhoaHayNguoiTao() throws Exception {
        themBai("may-tinh-la-gi", "PUBLISHED", "x");

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user(hocSinh())))
                .andExpect(jsonPath("$.*", hasSize(9)))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.courseId").doesNotExist())
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void baiNhapBi404VaKhongLoNoiDungNhap() throws Exception {
        themBai("bai-nhap", "DRAFT", "BAN-NHAP-CHUA-DANG-BI-MAT");

        MvcResult result = mockMvc.perform(get("/api/lessons/bai-nhap").with(user(hocSinh())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy bài học"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("BAN-NHAP-CHUA-DANG-BI-MAT");
    }

    @Test
    void slugKhongTonTaiVaBaiNhapCungMotCau404NenKhongDoDuocBaiNhap() throws Exception {
        themBai("bai-nhap", "DRAFT", "x");

        String chiTietBaiNhap = mockMvc.perform(get("/api/lessons/bai-nhap").with(user(hocSinh())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String chiTietSlugLa = mockMvc.perform(get("/api/lessons/khong-ton-tai").with(user(hocSinh())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Chỉ khác trường instance (đường dẫn của chính yêu cầu); phần còn lại phải giống hệt.
        assertThat(chiTietBaiNhap.replace("bai-nhap", "SLUG"))
                .isEqualTo(chiTietSlugLa.replace("khong-ton-tai", "SLUG"));
    }

    @Test
    void khachChuaDangNhapBi401KhongDocDuocNoiDungBaiDaDang() throws Exception {
        themBai("may-tinh-la-gi", "PUBLISHED", "NOI-DUNG-CHI-DANH-CHO-NGUOI-DA-DANG-NHAP");

        MvcResult result = mockMvc.perform(get("/api/lessons/may-tinh-la-gi"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("NOI-DUNG-CHI-DANH-CHO-NGUOI-DA-DANG-NHAP");
    }
}
