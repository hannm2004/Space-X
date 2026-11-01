package vn.spacex.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.health.HealthController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Kiểm tra luật URL /api/admin/** (D26). Role giả lập khớp với AppUserDetails: Spring tự thêm tiền tố ROLE_.
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, AdminAuthorizationTest.GiaController.class})
class AdminAuthorizationTest {

    // Endpoint chỉ tồn tại trong test; đại diện cho API Admin và API thường. Không đưa vào mã thật.
    @RestController
    static class GiaController {

        @GetMapping("/api/admin/ping")
        String adminPing() {
            return "ok";
        }

        @GetMapping("/api/khac/ping")
        String khacPing() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chuaDangNhapGoiApiAdminTra401() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "hocsinh@example.com", roles = "LEARNER")
    void hocSinhGoiApiAdminTra403DangProblemJson() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Bạn không có quyền thực hiện thao tác này"));
    }

    @Test
    @WithMockUser(username = "hocsinh@example.com", roles = "LEARNER")
    void thongBao403KhongLoEmailHayVaiTro() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("hocsinh@example.com"))))
                .andExpect(content().string(not(containsString("LEARNER"))))
                .andExpect(content().string(not(containsString("ADMIN"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminGoiApiAdminTra200() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void luatAdminKhongChanApiThuongCuaHocSinh() throws Exception {
        mockMvc.perform(get("/api/khac/ping"))
                .andExpect(status().isOk());
    }

    // Chặn xảy ra ở bộ lọc, trước khi tìm controller, nên "/api/admin" và "/api/admin/" không được lọt qua.
    @Test
    @WithMockUser(roles = "LEARNER")
    void hocSinhGoiGocAdminKhongCoHoacCoDauGachCheoVanBiChan() throws Exception {
        mockMvc.perform(get("/api/admin")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/")).andExpect(status().isForbidden());
    }
}
