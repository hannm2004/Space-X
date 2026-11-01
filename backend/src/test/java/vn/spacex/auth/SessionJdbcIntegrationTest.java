package vn.spacex.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Session nằm trong MySQL (Spring Session JDBC, migration V2) và CSRF đi bằng cookie/header thật, giống trình duyệt.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(SessionJdbcIntegrationTest.BaoVeController.class)
class SessionJdbcIntegrationTest {

    private static final String EMAIL = "a@example.com";
    private static final String MAT_KHAU = "matkhau123";

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinhDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    // Endpoint chỉ tồn tại trong test, đại diện cho mọi API cần đăng nhập; không đưa vào mã thật.
    @RestController
    static class BaoVeController {

        @GetMapping("/api/test/bao-ve")
        String baoVe() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM users");
    }

    // Lấy cookie XSRF-TOKEN như trình duyệt: gọi một GET công khai rồi gửi lại cả cookie lẫn header.
    private Cookie layXsrf() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/api/health")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        return xsrf;
    }

    private MvcResult postJson(String url, String json, Cookie... cookies) throws Exception {
        Cookie xsrf = layXsrf();
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(json)
                .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue());
        if (cookies.length > 0) {
            request = request.cookie(cookies);
        }
        return mockMvc.perform(request).andReturn();
    }

    private void dangKy() throws Exception {
        MvcResult result = postJson("/api/auth/register", """
                {"displayName":"A","email":"%s","password":"%s","confirmPassword":"%s"}"""
                .formatted(EMAIL, MAT_KHAU, MAT_KHAU));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
    }

    private MvcResult dangNhap(String matKhau) throws Exception {
        return postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(EMAIL, matKhau));
    }

    private int demSession() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class);
    }

    @Test
    void dangNhapBangLuongCsrfCookieThatThanhCongVaNhanCookieSession() throws Exception {
        dangKy();

        MvcResult result = dangNhap(MAT_KHAU);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getCookie("SESSION")).isNotNull();
        assertThat(result.getResponse().getCookie("JSESSIONID")).isNull();
    }

    @Test
    void dangNhapLuuSessionVaoMySqlGanVoiEmail() throws Exception {
        dangKy();

        dangNhap(MAT_KHAU);

        assertThat(demSession()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT PRINCIPAL_NAME FROM SPRING_SESSION", String.class)).isEqualTo(EMAIL);
    }

    @Test
    void cookieSessionDungDuocChoApiCanDangNhap() throws Exception {
        dangKy();
        Cookie session = dangNhap(MAT_KHAU).getResponse().getCookie("SESSION");

        mockMvc.perform(get("/api/test/bao-ve").cookie(session)).andExpect(status().isOk());
    }

    // Chứng minh nguồn sự thật là database chứ không phải bộ nhớ: xóa dòng session thì cookie cũ hết hiệu lực.
    @Test
    void xoaSessionTrongDatabaseThiCookieCuBi401() throws Exception {
        dangKy();
        Cookie session = dangNhap(MAT_KHAU).getResponse().getCookie("SESSION");
        jdbc.update("DELETE FROM SPRING_SESSION");

        mockMvc.perform(get("/api/test/bao-ve").cookie(session)).andExpect(status().isUnauthorized());
    }

    // Người chưa đăng nhập gọi API cần đăng nhập chỉ nhận 401. Spring mặc định lưu "yêu cầu đang dở" vào session để
    // quay lại sau khi đăng nhập, làm mỗi yêu cầu ẩn danh ghi thêm một dòng vào MySQL; API JSON không cần việc đó.
    @Test
    void yeuCauAnDanhBi401KhongTaoSessionVaKhongPhatCookieSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/bao-ve")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getCookie("SESSION")).isNull();
        assertThat(demSession()).isZero();
    }

    @Test
    void dangXuatXoaSessionKhoiDatabase() throws Exception {
        dangKy();
        Cookie session = dangNhap(MAT_KHAU).getResponse().getCookie("SESSION");
        assertThat(demSession()).isEqualTo(1);

        MvcResult result = postJson("/api/auth/logout", "", session);

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(demSession()).isZero();
    }

    @Test
    void dangNhapThatBaiKhongTaoSessionVaKhongPhatCookie() throws Exception {
        dangKy();

        MvcResult result = dangNhap("sai-mat-khau");

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getCookie("SESSION")).isNull();
        assertThat(demSession()).isZero();
    }

    // Session được tuần tự hóa vào cột BLOB: mã băm và mật khẩu không được nằm trong đó.
    @Test
    void sessionLuuTrongDatabaseKhongChuaMaBamHayMatKhau() throws Exception {
        dangKy();
        dangNhap(MAT_KHAU);

        List<byte[]> blobs = jdbc.queryForList("SELECT ATTRIBUTE_BYTES FROM SPRING_SESSION_ATTRIBUTES", byte[].class);

        assertThat(blobs).isNotEmpty();
        for (byte[] blob : blobs) {
            String noiDung = new String(blob, StandardCharsets.ISO_8859_1);
            assertThat(noiDung).doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain(MAT_KHAU);
        }
    }

    // users.email cho phép 255 ký tự (D25) nên cột PRINCIPAL_NAME của bảng session phải đủ rộng; mặc định của thư viện
    // chỉ 100 ký tự và sẽ làm đăng nhập lỗi 500 với email dài.
    @Test
    void emailDaiHon100KyTuVanDangNhapVaLuuDuocSession() throws Exception {
        String emailDai = "a".repeat(60) + "@" + ("b".repeat(60) + ".").repeat(3) + "com";
        assertThat(emailDai.length()).isBetween(101, 255);
        MvcResult dangKy = postJson("/api/auth/register", """
                {"displayName":"A","email":"%s","password":"%s","confirmPassword":"%s"}"""
                .formatted(emailDai, MAT_KHAU, MAT_KHAU));
        assertThat(dangKy.getResponse().getStatus()).isEqualTo(201);

        MvcResult result = postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(emailDai, MAT_KHAU));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT PRINCIPAL_NAME FROM SPRING_SESSION", String.class)).isEqualTo(emailDai);
    }

    @Test
    void cookieSessionCoHttpOnlyVaSameSiteLax() throws Exception {
        dangKy();

        MvcResult result = dangNhap(MAT_KHAU);

        List<String> setCookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String cookieSession = setCookie.stream().filter(c -> c.startsWith("SESSION=")).findFirst().orElseThrow();
        assertThat(cookieSession).contains("HttpOnly").contains("SameSite=Lax");
    }
}
