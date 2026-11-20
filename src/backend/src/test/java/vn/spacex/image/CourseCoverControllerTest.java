package vn.spacex.image;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.user.Role;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminCourseCoverController.class, CourseCoverController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CourseCoverControllerTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
    private static final String URL_BIA = "/api/courses/tin-hoc-10/cover?v=1763521200000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseCoverService service;

    private static AppUserDetails admin() {
        return new AppUserDetails(1L, "admin@example.com", null, "Quản trị viên", Role.ADMIN, true);
    }

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, "hs@example.com", null, "Học sinh", Role.LEARNER, true);
    }

    private static MockMultipartFile tep() {
        return new MockMultipartFile("file", "bia.png", "image/png", PNG);
    }

    // ---- Đặt ảnh bìa (Admin) ----

    @Test
    void khachHocSinhVaAdminThieuCsrfKhongDatAnhBiaDuoc() throws Exception {
        mockMvc.perform(multipart("/api/admin/courses/5/cover").file(tep()).with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/admin/courses/5/cover").file(tep()).with(user(hocSinh())).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(multipart("/api/admin/courses/5/cover").file(tep()).with(user(admin())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminDatAnhBiaThanhCongTraDuongDanMoiDungMotTruong() throws Exception {
        when(service.setCover(eq(5L), aryEq(PNG))).thenReturn(new CourseCoverResponse(URL_BIA));

        mockMvc.perform(multipart("/api/admin/courses/5/cover").file(tep()).with(user(admin())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").value(URL_BIA))
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).setCover(eq(5L), aryEq(PNG));
    }

    @Test
    void anhLonHon1MBBiChanTruocKhiGoiService() throws Exception {
        MockMultipartFile lon = new MockMultipartFile("file", "lon.png", "image/png", new byte[ImageService.MAX_BYTES + 1]);

        mockMvc.perform(multipart("/api/admin/courses/5/cover").file(lon).with(user(admin())).with(csrf()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.detail").value(ImageService.TOO_LARGE_DETAIL));

        verifyNoInteractions(service);
    }

    @Test
    void tepKhongPhaiAnhTraLoi415() throws Exception {
        when(service.setCover(eq(5L), aryEq(PNG))).thenThrow(new UnsupportedImageTypeException());

        mockMvc.perform(multipart("/api/admin/courses/5/cover").file(tep()).with(user(admin())).with(csrf()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void datAnhBiaChoKhoaKhongTonTaiTraLoi404() throws Exception {
        when(service.setCover(eq(99L), aryEq(PNG))).thenThrow(new CourseNotFoundException());

        mockMvc.perform(multipart("/api/admin/courses/99/cover").file(tep()).with(user(admin())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ---- Bỏ ảnh bìa (Admin) ----

    @Test
    void khachHocSinhVaAdminThieuCsrfKhongBoAnhBiaDuoc() throws Exception {
        mockMvc.perform(delete("/api/admin/courses/5/cover").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/courses/5/cover").with(user(hocSinh())).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/courses/5/cover").with(user(admin()))).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminBoAnhBiaTraLoi204() throws Exception {
        mockMvc.perform(delete("/api/admin/courses/5/cover").with(user(admin())).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).clearCover(5L);
    }

    @Test
    void boAnhBiaCuaKhoaKhongTonTaiTraLoi404() throws Exception {
        doThrow(new CourseNotFoundException()).when(service).clearCover(99L);

        mockMvc.perform(delete("/api/admin/courses/99/cover").with(user(admin())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ---- Xem ảnh bìa (công khai) ----

    @Test
    void khachChuaDangNhapXemDuocAnhBiaVoiLoaiAnhVaCacheCongKhai() throws Exception {
        when(service.load("tin-hoc-10")).thenReturn(new StoredImage("image/png", PNG));

        mockMvc.perform(get("/api/courses/tin-hoc-10/cover"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void khoaChuaCoAnhBiaHoacKhongTonTaiTraLoi404() throws Exception {
        when(service.load("chua-co")).thenThrow(new ImageNotFoundException());
        when(service.load("khong-co")).thenThrow(new CourseNotFoundException());

        mockMvc.perform(get("/api/courses/chua-co/cover")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/courses/khong-co/cover")).andExpect(status().isNotFound());
    }
}
