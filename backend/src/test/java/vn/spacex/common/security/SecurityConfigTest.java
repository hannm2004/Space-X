package vn.spacex.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.health.HealthController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Chỉ nạp HealthController, SecurityConfig và controller giả để test không phụ thuộc controller khác.
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, SecurityConfigTest.BaoVeController.class})
class SecurityConfigTest {

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

    @Test
    void healthCongKhaiKhongCanDangNhap() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void apiCanDangNhapTraVe401DangProblemJsonKhiChuaDangNhap() throws Exception {
        mockMvc.perform(get("/api/test/bao-ve"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }
}
