package vn.spacex.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(GlobalExceptionHandlerTest.LoiBatNgoController.class)
class GlobalExceptionHandlerTest {

    // Endpoint chỉ tồn tại trong test để ép server ném lỗi bất ngờ; không đưa vào mã thật.
    @RestController
    static class LoiBatNgoController {

        @GetMapping("/test/loi-bat-ngo")
        String loiBatNgo() {
            throw new RuntimeException("mat-khau-bi-mat");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void duongDanKhongTonTaiTraVe404DangProblemJson() throws Exception {
        mockMvc.perform(get("/api/khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void saiMethodTraVe405DangProblemJson() throws Exception {
        mockMvc.perform(post("/api/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void loiBatNgoTraVe500KhongLoChiTietNoiBo() throws Exception {
        mockMvc.perform(get("/test/loi-bat-ngo"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("mat-khau-bi-mat"))))
                .andExpect(content().string(not(containsString("RuntimeException"))));
    }
}
