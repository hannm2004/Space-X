package vn.spacex.auth;

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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Giới hạn số lần đăng nhập sai trên cả ứng dụng thật. Bộ đếm sống trong bộ nhớ suốt vòng đời ứng dụng, nên mỗi
// test dùng email và địa chỉ IP riêng để không đụng nhau.
@SpringBootTest(properties = {"app.login.max-failures-per-email=3", "app.login.max-failures-per-ip=6",
        "app.login.window=15m"})
@AutoConfigureMockMvc
@Testcontainers
class LoginRateLimitIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinhDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    private static final String MAT_KHAU = "mat-khau-dung-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM users");
    }

    private void dangKy(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"A","email":"%s","password":"%s","confirmPassword":"%s"}"""
                                .formatted(email, MAT_KHAU, MAT_KHAU)))
                .andExpect(status().isCreated());
    }

    private ResultActions dangNhap(String email, String matKhau, String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, matKhau)));
    }

    @Test
    void saiQuaSoLanChoPhepThiBi429KeCaKhiGoDungMatKhau() throws Exception {
        dangKy("mot@example.com");
        for (int i = 0; i < 3; i++) {
            dangNhap("mot@example.com", "sai-mat-khau", "10.0.0.1").andExpect(status().isUnauthorized());
        }

        dangNhap("mot@example.com", MAT_KHAU, "10.0.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Bạn đã đăng nhập sai quá nhiều lần. Vui lòng thử lại sau."));
        // Đổi địa chỉ IP cũng không né được giới hạn theo email.
        dangNhap("mot@example.com", MAT_KHAU, "10.0.0.2").andExpect(status().isTooManyRequests());
    }

    // Viết hoa hay thêm khoảng trắng vẫn là cùng một tài khoản.
    @Test
    void emailDuocChuanHoaTruocKhiDem() throws Exception {
        dangNhap("Hai@Example.com", "sai-mat-khau", "10.0.1.1").andExpect(status().isUnauthorized());
        dangNhap("  hai@example.com ", "sai-mat-khau", "10.0.1.1").andExpect(status().isUnauthorized());
        dangNhap("HAI@EXAMPLE.COM", "sai-mat-khau", "10.0.1.1").andExpect(status().isUnauthorized());

        dangNhap("hai@example.com", "sai-mat-khau", "10.0.1.1").andExpect(status().isTooManyRequests());
    }

    // Câu trả lời không được khác nhau giữa email có thật và email bịa (D25).
    @Test
    void emailKhongTonTaiBiGioiHanGiongHetEmailCoThat() throws Exception {
        dangKy("that@example.com");
        for (int i = 0; i < 3; i++) {
            dangNhap("that@example.com", "sai-mat-khau", "10.0.2.1");
            dangNhap("bia@example.com", "sai-mat-khau", "10.0.2.2");
        }

        String that = dangNhap("that@example.com", "sai-mat-khau", "10.0.2.1")
                .andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String bia = dangNhap("bia@example.com", "sai-mat-khau", "10.0.2.2")
                .andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(bia).isEqualTo(that);
    }

    @Test
    void dangNhapDungThiXoaSoLanSaiCuaEmailDo() throws Exception {
        dangKy("ba@example.com");
        dangNhap("ba@example.com", "sai-mat-khau", "10.0.3.1");
        dangNhap("ba@example.com", "sai-mat-khau", "10.0.3.1");
        dangNhap("ba@example.com", MAT_KHAU, "10.0.3.1").andExpect(status().isOk());

        dangNhap("ba@example.com", "sai-mat-khau", "10.0.3.1").andExpect(status().isUnauthorized());
        dangNhap("ba@example.com", "sai-mat-khau", "10.0.3.1").andExpect(status().isUnauthorized());
        dangNhap("ba@example.com", MAT_KHAU, "10.0.3.1").andExpect(status().isOk());
    }

    // Một máy thử lần lượt nhiều tài khoản cũng bị chặn.
    @Test
    void motDiaChiThuNhieuEmailCungBiChan() throws Exception {
        for (int i = 0; i < 6; i++) {
            dangNhap("do-" + i + "@example.com", "sai-mat-khau", "10.0.4.1").andExpect(status().isUnauthorized());
        }

        dangNhap("do-moi@example.com", "sai-mat-khau", "10.0.4.1").andExpect(status().isTooManyRequests());
        dangNhap("do-moi@example.com", "sai-mat-khau", "10.0.4.2").andExpect(status().isUnauthorized());
    }

    // Đăng nhập đúng không tính vào hạn mức của địa chỉ (phòng máy chung của cả lớp).
    @Test
    void dangNhapDungKhongTonHanMucCuaDiaChi() throws Exception {
        dangKy("bon@example.com");
        for (int i = 0; i < 10; i++) {
            dangNhap("bon@example.com", MAT_KHAU, "10.0.5.1").andExpect(status().isOk());
        }
    }
}
