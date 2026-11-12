package vn.spacex.progress;

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
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.lesson.LessonNotFoundException;
import vn.spacex.user.Role;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProgressControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressService progressService;

    private static AppUserDetails nguoiDung() {
        return new AppUserDetails(USER_ID, "a@example.com", null, "Nguyễn Văn A", Role.LEARNER, true);
    }

    // ---- Phải đăng nhập, và yêu cầu ghi phải có CSRF ----

    @Test
    void chuaDangNhapCaBaDuongDanDeuBi401VaKhongGoiService() throws Exception {
        mockMvc.perform(get("/api/me/progress"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/me/progress/lessons/11").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/me/progress/courses/3").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(progressService);
    }

    @Test
    void thieuCsrfThiGhiVaXoaBi403VaKhongGoiService() throws Exception {
        mockMvc.perform(put("/api/me/progress/lessons/11").with(user(nguoiDung()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/me/progress/courses/3").with(user(nguoiDung())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(progressService);
    }

    // ---- GET /api/me/progress ----

    @Test
    void xemTienDoTraVeMangTungKhoaVoiDayDuBayTruongVaHocTiep() throws Exception {
        when(progressService.overview(USER_ID)).thenReturn(List.of(
                new CourseProgressResponse(3L, "tin-hoc-10", "Tin học 10", 1, 3, 33,
                        new ContinueLessonResponse(12L, "mang-may-tinh", "Mạng máy tính")),
                new CourseProgressResponse(4L, "tin-hoc-11", "Tin học 11", 0, 0, 0, null)));

        mockMvc.perform(get("/api/me/progress").with(user(nguoiDung())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].courseId").value(3))
                .andExpect(jsonPath("$[0].courseSlug").value("tin-hoc-10"))
                .andExpect(jsonPath("$[0].courseTitle").value("Tin học 10"))
                .andExpect(jsonPath("$[0].completedLessons").value(1))
                .andExpect(jsonPath("$[0].totalLessons").value(3))
                .andExpect(jsonPath("$[0].percent").value(33))
                .andExpect(jsonPath("$[0].continueLesson.id").value(12))
                .andExpect(jsonPath("$[0].continueLesson.slug").value("mang-may-tinh"))
                .andExpect(jsonPath("$[0].continueLesson.title").value("Mạng máy tính"))
                .andExpect(jsonPath("$[1].continueLesson").doesNotExist());
    }

    // Người dùng hiện tại luôn lấy từ session (CLAUDE.md): tham số userId gửi lên bị bỏ qua.
    @Test
    void xemTienDoChiDungIdTuSessionKhongTinUserIdGuiLen() throws Exception {
        when(progressService.overview(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/me/progress").param("userId", "999").with(user(nguoiDung())))
                .andExpect(status().isOk());

        verify(progressService).overview(USER_ID);
        verify(progressService, never()).overview(999L);
    }

    // ---- PUT /api/me/progress/lessons/{lessonId} ----

    @Test
    void ghiHoanThanhTraVeDungHaiTruongVaDungIdTuSession() throws Exception {
        when(progressService.record(USER_ID, 11L, ProgressStatus.COMPLETED))
                .thenReturn(new LessonProgressResponse(11L, ProgressStatus.COMPLETED));

        mockMvc.perform(put("/api/me/progress/lessons/11").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(11))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(progressService).record(USER_ID, 11L, ProgressStatus.COMPLETED);
    }

    @Test
    void ghiTienDoBoQuaUserIdVaCacTruongLaTrongThan() throws Exception {
        when(progressService.record(USER_ID, 11L, ProgressStatus.IN_PROGRESS))
                .thenReturn(new LessonProgressResponse(11L, ProgressStatus.IN_PROGRESS));

        mockMvc.perform(put("/api/me/progress/lessons/11").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"userId\":999,\"completedAt\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isOk());

        verify(progressService).record(USER_ID, 11L, ProgressStatus.IN_PROGRESS);
        verify(progressService, never()).record(eq(999L), anyLong(), any());
    }

    @Test
    void trangThaiThieuSaiHoacThanRongThiBi400VaKhongGoiService() throws Exception {
        for (String than : List.of("{}", "{\"status\":null}", "{\"status\":\"DONE\"}", "{\"status\":\"completed\"}", "")) {
            mockMvc.perform(put("/api/me/progress/lessons/11").with(user(nguoiDung())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(than))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(progressService);
    }

    @Test
    void lessonIdKhongPhaiSoThi400VoiCauCoDinhKhongLapLaiGiaTriNguoiGui() throws Exception {
        mockMvc.perform(put("/api/me/progress/lessons/abc").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tham số trong đường dẫn không hợp lệ"));

        verifyNoInteractions(progressService);
    }

    @Test
    void baiKhongTonTaiHoacChuaDangThi404() throws Exception {
        when(progressService.record(USER_ID, 99L, ProgressStatus.COMPLETED)).thenThrow(new LessonNotFoundException());

        mockMvc.perform(put("/api/me/progress/lessons/99").with(user(nguoiDung())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/me/progress/courses/{courseId} ----

    @Test
    void hocLaiKhoaTra204KhongCoThanVaXoaChoDungNguoiDung() throws Exception {
        mockMvc.perform(delete("/api/me/progress/courses/3").with(user(nguoiDung())).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(progressService).restartCourse(USER_ID, 3L);
    }

    @Test
    void hocLaiKhoaKhongTonTaiThi404() throws Exception {
        doThrow(new CourseNotFoundException()).when(progressService).restartCourse(USER_ID, 99L);

        mockMvc.perform(delete("/api/me/progress/courses/99").with(user(nguoiDung())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void courseIdKhongPhaiSoThi400VoiCauCoDinhVaKhongGoiService() throws Exception {
        mockMvc.perform(delete("/api/me/progress/courses/abc").with(user(nguoiDung())).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tham số trong đường dẫn không hợp lệ"));

        verifyNoInteractions(progressService);
    }
}
