package vn.spacex.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.health.HealthController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dùng src/test/resources/static/index.html và static/assets/app.js làm "frontend đã build".
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, SpaWebConfig.class})
class SpaWebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void trangChuDuocChuyenTiepToiIndexHtml() throws Exception {
        // Spring Boot xử lý "/" bằng một lần forward tới index.html (MockMvc không tự đi theo forward).
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void indexHtmlTraVeDungNoiDung() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SPA-INDEX")));
    }

    @Test
    void duongDanCuaReactRouterTraVeIndexHtmlDeF5KhongBi404() throws Exception {
        mockMvc.perform(get("/khoa-hoc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SPA-INDEX")));
    }

    @Test
    void duongDanLongNhauCuaReactRouterCungTraVeIndexHtml() throws Exception {
        mockMvc.perform(get("/khoa-hoc/tin-hoc-10/bai/thong-tin-va-du-lieu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SPA-INDEX")));
    }

    @Test
    void khachChuaDangNhapVaoTrangConVanNhanIndexHtml() throws Exception {
        // Quyền đọc bài do API quyết định (401), không phải do việc nạp trang React.
        mockMvc.perform(get("/dang-nhap"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SPA-INDEX")));
    }

    @Test
    void fileTinhCoThatDuocPhucVuNguyenVanKhongBiThayBangIndexHtml() throws Exception {
        mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("APP-JS")))
                .andExpect(content().string(not(containsString("SPA-INDEX"))));
    }

    @Test
    void fileTinhKhongTonTaiTraVe404ThayViIndexHtml() throws Exception {
        mockMvc.perform(get("/assets/khong-co.js"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/favicon-khong-co.ico"))
                .andExpect(status().isNotFound());
    }

    @Test
    void apiKhongTonTaiVoiKhachVanLa401KhongPhaiIndexHtml() throws Exception {
        mockMvc.perform(get("/api/khong-co"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("SPA-INDEX"))));
    }

    @Test
    @WithMockUser
    void apiKhongTonTaiVoiNguoiDaDangNhapLa404KhongPhaiIndexHtml() throws Exception {
        mockMvc.perform(get("/api/khong-co"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("SPA-INDEX"))));
    }

    @Test
    void apiThatVanHoatDongBinhThuong() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void yeuCauGhiVaoDuongDanTrangKhongBiBienThanhIndexHtml() throws Exception {
        mockMvc.perform(post("/khoa-hoc").with(csrf()))
                .andExpect(status().is4xxClientError());
    }
}
