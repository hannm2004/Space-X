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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng với MySQL 8.4 thật: ADMIN tạo lúc khởi động, đăng nhập bằng session thật rồi tải/xem ảnh.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ImageIntegrationTest {

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
    private long baiNhap;
    private long baiDaDang;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lesson_images");
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users WHERE role = 'LEARNER'");
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE role = 'ADMIN'", Long.class);
        jdbc.update("INSERT INTO courses (slug, title, sort_order) VALUES ('tin-hoc-10', 'Tin học 10', 1)");
        long khoa = jdbc.queryForObject("SELECT id FROM courses WHERE slug = 'tin-hoc-10'", Long.class);
        baiNhap = themBai(khoa, "bai-nhap", "DRAFT");
        baiDaDang = themBai(khoa, "bai-da-dang", "PUBLISHED");
    }

    private long themBai(long khoaId, String slug, String trangThai) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, content_md, status, sort_order)
                VALUES (?, ?, ?, 'Bài', 'x', ?, 1)""", khoaId, adminId, slug, trangThai);
        return jdbc.queryForObject("SELECT id FROM lessons WHERE slug = ?", Long.class, slug);
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

    private MvcResult taiLen(Cookie admin, long baiId, String tenTep, String contentTypeKhaiBao, byte[] bytes)
            throws Exception {
        return mockMvc.perform(multipart("/api/admin/lessons/" + baiId + "/images")
                .file(new MockMultipartFile("file", tenTep, contentTypeKhaiBao, bytes))
                .cookie(admin).with(csrf())).andReturn();
    }

    private long taiAnhHopLe(Cookie admin, long baiId) throws Exception {
        MvcResult r = taiLen(admin, baiId, "so-do.png", "image/png", ImageInspectorTest.png(20, 10));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        return Long.parseLong(r.getResponse().getContentAsString(StandardCharsets.UTF_8)
                .replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private int demAnh() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM lesson_images", Integer.class);
    }

    // ---- Tải lên ----

    @Test
    void adminTaiPngThatGhiDungVaoMySqlVaTraUrlDeChenMarkdown() throws Exception {
        byte[] png = ImageInspectorTest.png(20, 10);
        Cookie admin = dangNhapAdmin();

        mockMvc.perform(multipart("/api/admin/lessons/" + baiNhap + "/images")
                        .file(new MockMultipartFile("file", "so-do.png", "image/png", png))
                        .cookie(admin).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", containsString("/api/images/")));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT lesson_id, file_name, content_type, size_bytes, data FROM lesson_images");
        assertThat(((Number) row.get("lesson_id")).longValue()).isEqualTo(baiNhap);
        assertThat(row.get("file_name")).isEqualTo("so-do.png");
        assertThat(row.get("content_type")).isEqualTo("image/png");
        assertThat(row.get("size_bytes")).isEqualTo(png.length);
        assertThat((byte[]) row.get("data")).isEqualTo(png);
    }

    @Test
    void loaiLuuLaLoaiThatCuaNoiDungKhongTinDuoiTepVaContentTypeKhaiBao() throws Exception {
        Cookie admin = dangNhapAdmin();

        MvcResult r = taiLen(admin, baiNhap, "that-ra-la-jpeg.png", "image/gif", ImageInspectorTest.jpeg(20, 10));

        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT content_type FROM lesson_images", String.class)).isEqualTo("image/jpeg");
    }

    @Test
    void tepGiaAnhBi415VaKhongGhiGiDuDuoiVaContentTypeNoiLaPng() throws Exception {
        Cookie admin = dangNhapAdmin();
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(taiLen(admin, baiNhap, "x.png", "image/png", html).getResponse().getStatus()).isEqualTo(415);
        assertThat(taiLen(admin, baiNhap, "x.svg", "image/svg+xml", svg).getResponse().getStatus()).isEqualTo(415);
        assertThat(taiLen(admin, baiNhap, "x.png", "image/png", "GIF89a....".getBytes()).getResponse().getStatus())
                .isEqualTo(415);
        assertThat(taiLen(admin, baiNhap, "rong.png", "image/png", new byte[0]).getResponse().getStatus())
                .isEqualTo(415);
        MvcResult r = taiLen(admin, baiNhap, "x.png", "image/png", html);
        assertThat(r.getResponse().getContentType()).contains("application/problem+json");
        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("Chỉ nhận ảnh PNG, JPEG hoặc WebP hợp lệ");

        assertThat(demAnh()).isZero();
    }

    @Test
    void anhVuot1MBBi413VaKhongGhiGi() throws Exception {
        Cookie admin = dangNhapAdmin();
        byte[] quaLon = new byte[1_048_577];
        System.arraycopy(ImageInspectorTest.png(10, 10), 0, quaLon, 0, 8);

        MvcResult r = taiLen(admin, baiNhap, "to.png", "image/png", quaLon);

        assertThat(r.getResponse().getStatus()).isEqualTo(413);
        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("Ảnh vượt quá 1 MB");
        assertThat(demAnh()).isZero();
    }

    @Test
    void anhNenManhCoKichThuocDiemAnhKhongLoBiTuChoi() throws Exception {
        Cookie admin = dangNhapAdmin();

        MvcResult r = taiLen(admin, baiNhap, "bom.png", "image/png", ImageInspectorTest.pngNenManh(6000, 6000));

        assertThat(r.getResponse().getStatus()).isEqualTo(415);
        assertThat(demAnh()).isZero();
    }

    @Test
    void tenTepCoDuongDanChiLuuPhanCuoi() throws Exception {
        Cookie admin = dangNhapAdmin();

        taiLen(admin, baiNhap, "../../etc/passwd.png", "image/png", ImageInspectorTest.png(5, 5));

        assertThat(jdbc.queryForObject("SELECT file_name FROM lesson_images", String.class)).isEqualTo("passwd.png");
    }

    @Test
    void taiAnhVaoBaiKhongTonTaiBi404() throws Exception {
        Cookie admin = dangNhapAdmin();

        MvcResult r = taiLen(admin, 999999, "a.png", "image/png", ImageInspectorTest.png(5, 5));

        assertThat(r.getResponse().getStatus()).isEqualTo(404);
        assertThat(demAnh()).isZero();
    }

    @Test
    void khachVaHocSinhKhongTaiAnhLenDuoc() throws Exception {
        Cookie hs = dangNhapHocSinh();
        byte[] png = ImageInspectorTest.png(5, 5);

        mockMvc.perform(multipart("/api/admin/lessons/" + baiNhap + "/images")
                        .file(new MockMultipartFile("file", "a.png", "image/png", png)).with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/admin/lessons/" + baiNhap + "/images")
                        .file(new MockMultipartFile("file", "a.png", "image/png", png)).cookie(hs).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(demAnh()).isZero();
    }

    // ---- Xem ảnh: quyền theo trạng thái bài ----

    @Test
    void hocSinhXemDuocAnhCuaBaiDaDangVoiDungLoaiVaDuLieu() throws Exception {
        Cookie admin = dangNhapAdmin();
        Cookie hs = dangNhapHocSinh();
        byte[] png = ImageInspectorTest.png(20, 10);
        MvcResult up = taiLen(admin, baiDaDang, "a.png", "image/png", png);
        long id = Long.parseLong(up.getResponse().getContentAsString(StandardCharsets.UTF_8)
                .replaceAll(".*\"id\":(\\d+).*", "$1"));

        MvcResult r = mockMvc.perform(get("/api/images/" + id).cookie(hs))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andReturn();

        assertThat(r.getResponse().getContentAsByteArray()).isEqualTo(png);
    }

    @Test
    void hocSinhKhongXemDuocAnhCuaBaiNhapVaPhanHoiGiongHetAnhKhongTonTai() throws Exception {
        Cookie admin = dangNhapAdmin();
        Cookie hs = dangNhapHocSinh();
        long idNhap = taiAnhHopLe(admin, baiNhap);

        MvcResult nhap = mockMvc.perform(get("/api/images/" + idNhap).cookie(hs))
                .andExpect(status().isNotFound()).andReturn();
        MvcResult khongCo = mockMvc.perform(get("/api/images/999999").cookie(hs))
                .andExpect(status().isNotFound()).andReturn();

        String a = nhap.getResponse().getContentAsString(StandardCharsets.UTF_8).replace("/" + idNhap, "/X");
        String b = khongCo.getResponse().getContentAsString(StandardCharsets.UTF_8).replace("/999999", "/X");
        assertThat(a).isEqualTo(b);
        assertThat(a).contains("Không tìm thấy ảnh");
    }

    @Test
    void anhTuBaiNhapHienRaSauKhiDangVaBienMatKhiAn() throws Exception {
        Cookie admin = dangNhapAdmin();
        Cookie hs = dangNhapHocSinh();
        long id = taiAnhHopLe(admin, baiNhap);

        mockMvc.perform(get("/api/images/" + id).cookie(hs)).andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/lessons/" + baiNhap + "/status").cookie(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/images/" + id).cookie(hs)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/lessons/" + baiNhap + "/status").cookie(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/images/" + id).cookie(hs)).andExpect(status().isNotFound());
    }

    @Test
    void adminXemDuocAnhCuaBaiNhap() throws Exception {
        Cookie admin = dangNhapAdmin();
        long id = taiAnhHopLe(admin, baiNhap);

        mockMvc.perform(get("/api/images/" + id).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void khachChuaDangNhapKhongXemDuocKeCaAnhCuaBaiDaDang() throws Exception {
        Cookie admin = dangNhapAdmin();
        long id = taiAnhHopLe(admin, baiDaDang);

        mockMvc.perform(get("/api/images/" + id)).andExpect(status().isUnauthorized());
    }

    // ---- Danh sách và xóa ----

    @Test
    void adminLietKeAnhCuaBaiKhongKemDuLieuHocSinhKhongLietKeDuoc() throws Exception {
        Cookie admin = dangNhapAdmin();
        Cookie hs = dangNhapHocSinh();
        taiAnhHopLe(admin, baiNhap);
        taiAnhHopLe(admin, baiNhap);
        taiAnhHopLe(admin, baiDaDang);

        mockMvc.perform(get("/api/admin/lessons/" + baiNhap + "/images").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fileName").value("so-do.png"))
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andExpect(jsonPath("$[0].data").doesNotExist());
        mockMvc.perform(get("/api/admin/lessons/" + baiNhap + "/images").cookie(hs))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/lessons/999999/images").cookie(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void xoaAnhChiXoaDungAnhDoVaAnhXoaRoiThiXemLa404() throws Exception {
        Cookie admin = dangNhapAdmin();
        long a = taiAnhHopLe(admin, baiNhap);
        long b = taiAnhHopLe(admin, baiNhap);

        mockMvc.perform(delete("/api/admin/images/" + a).cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(demAnh()).isEqualTo(1);
        mockMvc.perform(get("/api/images/" + a).cookie(admin)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/images/" + b).cookie(admin)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/images/" + a).cookie(admin).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void hocSinhVaKhachKhongXoaDuocAnhVaThieuCsrfBi403() throws Exception {
        Cookie admin = dangNhapAdmin();
        Cookie hs = dangNhapHocSinh();
        long id = taiAnhHopLe(admin, baiNhap);

        mockMvc.perform(delete("/api/admin/images/" + id).with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/images/" + id).cookie(hs).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/images/" + id).cookie(admin)).andExpect(status().isForbidden());

        assertThat(demAnh()).isEqualTo(1);
    }

    @Test
    void xoaBaiKeoTheoAnhCuaBaiNhungKhongXoaAnhBaiKhac() throws Exception {
        Cookie admin = dangNhapAdmin();
        taiAnhHopLe(admin, baiNhap);
        taiAnhHopLe(admin, baiDaDang);

        mockMvc.perform(delete("/api/admin/lessons/" + baiNhap).cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(demAnh()).isEqualTo(1);
        assertThat(((Number) jdbc.queryForObject("SELECT lesson_id FROM lesson_images", Long.class)).longValue())
                .isEqualTo(baiDaDang);
    }
}
