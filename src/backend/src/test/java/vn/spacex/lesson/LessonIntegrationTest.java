package vn.spacex.lesson;

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

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users");
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, enabled)
                VALUES ('tacgia@example.com', 'khong-dung-de-dang-nhap', 'Tác giả', 'ADMIN', 'LOCAL', 1)""");
        tacGiaId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'tacgia@example.com'", Long.class);
        jdbc.update("INSERT INTO courses (slug, title, description, sort_order) VALUES ('tin-hoc-10', 'Tin học 10', 'mo ta', 1)");
        khoaId = jdbc.queryForObject("SELECT id FROM courses WHERE slug = 'tin-hoc-10'", Long.class);
    }

    private void themBai(String slug, String trangThai, String noiDung) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)""",
                khoaId, tacGiaId, slug, "Bài " + slug, "Tóm tắt " + slug, noiDung, trangThai);
    }

    @Test
    void baiDaDangTraNoiDungMarkdownNguyenVanVaTiengVietCoDauKhongBiHong() throws Exception {
        String markdown = "# Máy tính là gì?\n\nMáy tính xử lý **thông tin** theo chương trình.\n\n- Phần cứng\n- Phần mềm";
        themBai("may-tinh-la-gi", "PUBLISHED", markdown);

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user("hocsinh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("may-tinh-la-gi"))
                .andExpect(jsonPath("$.title").value("Bài may-tinh-la-gi"))
                .andExpect(jsonPath("$.summary").value("Tóm tắt may-tinh-la-gi"))
                .andExpect(jsonPath("$.contentMd").value(markdown));
    }

    @Test
    void phanHoiChiCoNamTruongKhongLoTrangThaiMaKhoaHayNguoiTao() throws Exception {
        themBai("may-tinh-la-gi", "PUBLISHED", "x");

        mockMvc.perform(get("/api/lessons/may-tinh-la-gi").with(user("hocsinh")))
                .andExpect(jsonPath("$.*", hasSize(5)));
    }

    @Test
    void baiNhapBi404VaKhongLoNoiDungNhap() throws Exception {
        themBai("bai-nhap", "DRAFT", "BAN-NHAP-CHUA-DANG-BI-MAT");

        MvcResult result = mockMvc.perform(get("/api/lessons/bai-nhap").with(user("hocsinh")))
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

        String chiTietBaiNhap = mockMvc.perform(get("/api/lessons/bai-nhap").with(user("hocsinh")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String chiTietSlugLa = mockMvc.perform(get("/api/lessons/khong-ton-tai").with(user("hocsinh")))
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
