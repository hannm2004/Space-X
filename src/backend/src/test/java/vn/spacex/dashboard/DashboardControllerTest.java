package vn.spacex.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.user.Role;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService service;

    private static AppUserDetails admin() {
        return new AppUserDetails(1L, "admin@example.com", null, "Quản trị viên", Role.ADMIN, true);
    }

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, "hs@example.com", null, "Học sinh", Role.LEARNER, true);
    }

    private static DashboardResponse mau() {
        return new DashboardResponse(
                new DashboardResponse.UserStats(12, 3),
                new DashboardResponse.LessonStats(7, 2, 5),
                List.of(new DashboardResponse.CourseStats(1L, "tin-hoc-10", "Tin học 10", 1, 3)),
                new DashboardResponse.LearningStats(4, 9));
    }

    @Test
    void khachChuaDangNhapBi401VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void hocSinhBi403VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").with(user(hocSinh())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(service);
    }

    @Test
    void adminXemDuocSoLieuDungHinhDangTrongApiMd() throws Exception {
        when(service.summary()).thenReturn(mau());

        mockMvc.perform(get("/api/admin/dashboard").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.total").value(12))
                .andExpect(jsonPath("$.users.newLast7Days").value(3))
                .andExpect(jsonPath("$.lessons.total").value(7))
                .andExpect(jsonPath("$.lessons.draft").value(2))
                .andExpect(jsonPath("$.lessons.published").value(5))
                .andExpect(jsonPath("$.courses[0].courseId").value(1))
                .andExpect(jsonPath("$.courses[0].slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$.courses[0].title").value("Tin học 10"))
                .andExpect(jsonPath("$.courses[0].draft").value(1))
                .andExpect(jsonPath("$.courses[0].published").value(3))
                .andExpect(jsonPath("$.learning.activeLearners").value(4))
                .andExpect(jsonPath("$.learning.completedLessons").value(9));

        verify(service).summary();
    }

    // Dashboard chỉ có số tổng hợp: không được lộ email hay tên của bất kỳ ai.
    @Test
    void phanHoiKhongChuaDuLieuCaNhan() throws Exception {
        when(service.summary()).thenReturn(mau());

        mockMvc.perform(get("/api/admin/dashboard").with(user(admin())))
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(content().string(not(containsString("displayName"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void chiChoPhepGetCacPhuongThucGhiBiTuChoi() throws Exception {
        mockMvc.perform(post("/api/admin/dashboard").with(user(admin())).with(csrf()))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(service);
    }
}
