package vn.spacex.image;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng với MySQL 8.4 thật: Admin đặt ảnh bìa khóa học, khách chưa đăng nhập xem được (trang chủ công khai).
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CourseCoverIntegrationTest {

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

    private long khoa10;
    private long khoa11;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM course_covers");
        jdbc.update("DELETE FROM lesson_images");
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users WHERE role = 'LEARNER'");
        jdbc.update("INSERT INTO courses (slug, title, sort_order) VALUES ('tin-hoc-10', 'Tin học 10', 1)");
        jdbc.update("INSERT INTO courses (slug, title, sort_order) VALUES ('tin-hoc-11', 'Tin học 11', 2)");
        khoa10 = jdbc.queryForObject("SELECT id FROM courses WHERE slug = 'tin-hoc-10'", Long.class);
        khoa11 = jdbc.queryForObject("SELECT id FROM courses WHERE slug = 'tin-hoc-11'", Long.class);
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

    private MvcResult datBia(Cookie admin, long khoaId, String tenTep, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/admin/courses/" + khoaId + "/cover")
                .file(new MockMultipartFile("file", tenTep, "image/png", bytes))
                .cookie(admin).with(csrf())).andReturn();
    }

    private int demBia() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM course_covers", Integer.class);
    }

    @Test
    void adminDatAnhBiaGhiDungVaoMySqlVaKhachChuaDangNhapXemDuoc() throws Exception {
        byte[] png = ImageInspectorTest.png(32, 18);
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(multipart("/api/admin/courses/" + khoa10 + "/cover")
                        .file(new MockMultipartFile("file", "bia.png", "image/png", png))
                        .cookie(admin).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl", containsString("/api/courses/tin-hoc-10/cover?v=")));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT course_id, content_type, size_bytes, data FROM course_covers");
        assertThat(((Number) row.get("course_id")).longValue()).isEqualTo(khoa10);
        assertThat(row.get("content_type")).isEqualTo("image/png");
        assertThat(row.get("size_bytes")).isEqualTo(png.length);
        assertThat((byte[]) row.get("data")).isEqualTo(png);

        // Không gửi cookie nào: ảnh bìa khóa học công khai như danh sách khóa.
        mockMvc.perform(get("/api/courses/tin-hoc-10/cover"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    void danhSachKhoaCongKhaiCoCoverImageUrlChiChoKhoaDaCoAnh() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coverImageUrl").value(nullValue()))
                .andExpect(jsonPath("$[1].coverImageUrl").value(nullValue()));

        datBia(dangNhapAdmin(), khoa10, "bia.png", ImageInspectorTest.png(32, 18));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$[0].coverImageUrl", containsString("/api/courses/tin-hoc-10/cover?v=")))
                .andExpect(jsonPath("$[1].slug").value("tin-hoc-11"))
                .andExpect(jsonPath("$[1].coverImageUrl").value(nullValue()))
                // id, slug, title, description, coverImageUrl, lessonCount.
                .andExpect(jsonPath("$[0].length()").value(6));
    }

    @Test
    void anhBiaCuaKhoaNayKhongXuatHienOKhoaKhac() throws Exception {
        byte[] png = ImageInspectorTest.png(32, 18);
        datBia(dangNhapAdmin(), khoa10, "bia.png", png);

        mockMvc.perform(get("/api/courses/tin-hoc-11/cover")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/courses/khong-co/cover")).andExpect(status().isNotFound());
    }

    @Test
    void datAnhMoiThayAnhCuMoiKhoaChiMotDongVaLuuLoaiThat() throws Exception {
        Cookie admin = dangNhapAdmin();
        datBia(admin, khoa10, "cu.png", ImageInspectorTest.png(32, 18));
        byte[] jpeg = ImageInspectorTest.jpeg(32, 18);

        MvcResult r = datBia(admin, khoa10, "that-ra-la-jpeg.png", jpeg);

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(demBia()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT content_type FROM course_covers", String.class)).isEqualTo("image/jpeg");
        mockMvc.perform(get("/api/courses/tin-hoc-10/cover"))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpeg));
    }

    @Test
    void boAnhBiaXoaDongVaAnhKhongConXemDuoc() throws Exception {
        Cookie admin = dangNhapAdmin();
        datBia(admin, khoa10, "bia.png", ImageInspectorTest.png(32, 18));

        mockMvc.perform(delete("/api/admin/courses/" + khoa10 + "/cover").cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());
        // Bỏ lần nữa khi đã hết ảnh vẫn là 204, không phải lỗi.
        mockMvc.perform(delete("/api/admin/courses/" + khoa10 + "/cover").cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(demBia()).isZero();
        mockMvc.perform(get("/api/courses/tin-hoc-10/cover")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/courses")).andExpect(jsonPath("$[0].coverImageUrl").value(nullValue()));
    }

    @Test
    void adminXemDanhSachKhoaCoCoverImageUrl() throws Exception {
        Cookie admin = dangNhapAdmin();
        datBia(admin, khoa11, "bia.png", ImageInspectorTest.png(32, 18));

        mockMvc.perform(get("/api/admin/courses").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coverImageUrl").value(nullValue()))
                .andExpect(jsonPath("$[1].coverImageUrl", containsString("/api/courses/tin-hoc-11/cover?v=")));
    }

    @Test
    void xoaKhoaKeoTheoAnhBiaDoKhoaNgoaiCascade() throws Exception {
        Cookie admin = dangNhapAdmin();
        datBia(admin, khoa10, "bia.png", ImageInspectorTest.png(32, 18));

        mockMvc.perform(delete("/api/admin/courses/" + khoa10).cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(demBia()).isZero();
    }

    @Test
    void tepGiaAnhBi415VaAnhLonBi413VaKhongGhiGi() throws Exception {
        Cookie admin = dangNhapAdmin();
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(datBia(admin, khoa10, "x.png", html).getResponse().getStatus()).isEqualTo(415);
        assertThat(datBia(admin, khoa10, "x.svg", svg).getResponse().getStatus()).isEqualTo(415);
        assertThat(datBia(admin, khoa10, "rong.png", new byte[0]).getResponse().getStatus()).isEqualTo(415);
        assertThat(datBia(admin, khoa10, "lon.png", new byte[ImageService.MAX_BYTES + 1]).getResponse().getStatus())
                .isEqualTo(413);

        assertThat(demBia()).isZero();
    }

    @Test
    void datAnhBiaChoKhoaKhongTonTaiTra404() throws Exception {
        MvcResult r = datBia(dangNhapAdmin(), 999_999L, "bia.png", ImageInspectorTest.png(32, 18));

        assertThat(r.getResponse().getStatus()).isEqualTo(404);
        assertThat(demBia()).isZero();
    }

    @Test
    void khachVaHocSinhKhongDatHoacBoAnhBiaDuoc() throws Exception {
        byte[] png = ImageInspectorTest.png(32, 18);

        mockMvc.perform(multipart("/api/admin/courses/" + khoa10 + "/cover")
                        .file(new MockMultipartFile("file", "bia.png", "image/png", png)).with(csrf()))
                .andExpect(status().isUnauthorized());
        Cookie hocSinh = dangNhapHocSinh();
        assertThat(datBia(hocSinh, khoa10, "bia.png", png).getResponse().getStatus()).isEqualTo(403);
        mockMvc.perform(delete("/api/admin/courses/" + khoa10 + "/cover").cookie(hocSinh).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(demBia()).isZero();
    }
}
