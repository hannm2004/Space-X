package vn.spacex.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Chạy trên Tomcat thật (cổng ngẫu nhiên) chứ không dùng MockMvc: thứ tự bộ lọc trong MockMvc khác server thật nên
// cờ Secure của cookie SESSION không phản ánh đúng. Render dừng TLS ở proxy và gửi HTTP thường kèm X-Forwarded-Proto: https;
// nếu ứng dụng không đọc header này thì cookie không có Secure và trình duyệt có thể gửi nó qua HTTP thường.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecureCookieRealServerIntegrationTest {

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

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM users");
    }

    // forwardedProto null = không có proxy (chạy http://localhost ở máy).
    private HttpResponse<String> gui(String method, String path, String json, String xsrf, String forwardedProto)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) {
            builder.header("Content-Type", "application/json");
        }
        if (xsrf != null) {
            builder.header("Cookie", "XSRF-TOKEN=" + xsrf).header("X-XSRF-TOKEN", xsrf);
        }
        if (forwardedProto != null) {
            builder.header("X-Forwarded-Proto", forwardedProto);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String dongSetCookie(HttpResponse<String> response, String ten) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        return cookies.stream().filter(c -> c.startsWith(ten + "=")).findFirst().orElseThrow();
    }

    private static String giaTri(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private String xsrfTuServer(String forwardedProto) throws Exception {
        return giaTri(dongSetCookie(gui("GET", "/api/health", null, null, forwardedProto), "XSRF-TOKEN"));
    }

    // Đăng ký rồi đăng nhập qua proxy (hoặc không); trả về phản hồi đăng nhập.
    private HttpResponse<String> dangKyVaDangNhap(String forwardedProto) throws Exception {
        String xsrf = xsrfTuServer(forwardedProto);
        HttpResponse<String> dangKy = gui("POST", "/api/auth/register", """
                {"displayName":"A","email":"%s","password":"%s","confirmPassword":"%s"}"""
                .formatted(EMAIL, MAT_KHAU, MAT_KHAU), xsrf, forwardedProto);
        assertThat(dangKy.statusCode()).isEqualTo(201);
        HttpResponse<String> dangNhap = gui("POST", "/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(EMAIL, MAT_KHAU), xsrf, forwardedProto);
        assertThat(dangNhap.statusCode()).isEqualTo(200);
        return dangNhap;
    }

    @Test
    void sauProxyHttpsCookieSessionVaXsrfTokenDeuCoSecure() throws Exception {
        assertThat(dongSetCookie(gui("GET", "/api/health", null, null, "https"), "XSRF-TOKEN")).contains("Secure");

        HttpResponse<String> dangNhap = dangKyVaDangNhap("https");

        assertThat(dongSetCookie(dangNhap, "SESSION")).contains("Secure").contains("HttpOnly").contains("SameSite=Lax");
    }

    // Chạy ở máy bằng http://localhost không có proxy: cookie Secure sẽ không được gửi lại nên không được bật cờ này.
    @Test
    void chayHttpThuongOMayThiCookieKhongCoSecure() throws Exception {
        assertThat(dongSetCookie(gui("GET", "/api/health", null, null, null), "XSRF-TOKEN")).doesNotContain("Secure");

        HttpResponse<String> dangNhap = dangKyVaDangNhap(null);

        assertThat(dongSetCookie(dangNhap, "SESSION")).doesNotContain("Secure").contains("HttpOnly");
    }
}
