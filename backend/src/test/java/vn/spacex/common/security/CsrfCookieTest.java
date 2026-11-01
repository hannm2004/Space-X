package vn.spacex.common.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.health.HealthController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CSRF theo docs/API.md: token nằm trong cookie XSRF-TOKEN (JavaScript đọc được) và phải gửi lại bằng header X-XSRF-TOKEN.
// Các test này cố ý KHÔNG dùng csrf() của spring-security-test vì nó bỏ qua cơ chế cookie thật.
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, CsrfCookieTest.GhiController.class})
class CsrfCookieTest {

    private static final String COOKIE = "XSRF-TOKEN";
    private static final String HEADER = "X-XSRF-TOKEN";
    private static final String TOKEN = "token-thu-nghiem";

    // Endpoint chỉ tồn tại trong test, đại diện cho mọi thao tác ghi cần đăng nhập; không đưa vào mã thật.
    @RestController
    static class GhiController {

        @PostMapping("/api/test/ghi")
        String ghi() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void yeuCauGetBatKyDeuNhanCookieXsrfTokenMaJavaScriptDocDuoc() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(cookie().exists(COOKIE))
                .andExpect(cookie().httpOnly(COOKIE, false))
                .andExpect(cookie().path(COOKIE, "/"));
    }

    @Test
    @WithMockUser
    void postCoCookieVaHeaderKhopNhauThiQua() throws Exception {
        mockMvc.perform(post("/api/test/ghi").cookie(new Cookie(COOKIE, TOKEN)).header(HEADER, TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void postDungTokenLayTuCookieThatCuaGetThiQua() throws Exception {
        MvcResult get = mockMvc.perform(get("/api/health")).andReturn();
        Cookie xsrf = get.getResponse().getCookie(COOKIE);
        assertThat(xsrf).isNotNull();

        mockMvc.perform(post("/api/test/ghi").cookie(xsrf).header(HEADER, xsrf.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void postCoCookieNhungThieuHeaderBi403DangProblemJson() throws Exception {
        mockMvc.perform(post("/api/test/ghi").cookie(new Cookie(COOKIE, TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser
    void postCoHeaderKhacCookieBi403() throws Exception {
        mockMvc.perform(post("/api/test/ghi").cookie(new Cookie(COOKIE, TOKEN)).header(HEADER, "token-gia"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void postCoHeaderNhungThieuCookieBi403() throws Exception {
        mockMvc.perform(post("/api/test/ghi").header(HEADER, TOKEN))
                .andExpect(status().isForbidden());
    }
}
