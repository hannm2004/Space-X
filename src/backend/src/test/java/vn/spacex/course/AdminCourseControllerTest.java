package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCourseController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminCourseControllerTest {

    private static final String BODY_HOP_LE = """
            {"slug":"tin-hoc-10","title":"Tin học 10","description":"Chương trình lớp 10","sortOrder":1}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCourseService service;

    private static AppUserDetails admin() {
        return new AppUserDetails(1L, "admin@example.com", null, "Quản trị viên", Role.ADMIN, true);
    }

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, "hs@example.com", null, "Học sinh", Role.LEARNER, true);
    }

    private static AdminCourseResponse khoa() {
        return new AdminCourseResponse(5L, "tin-hoc-10", "Tin học 10", "Chương trình lớp 10", 1);
    }

    // ---- Phân quyền và CSRF: luật /api/admin/** thật của SecurityConfig ----

    @Test
    void khachChuaDangNhapGoiBaThaoTacGhiDeuBi401VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(post("/api/admin/courses").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_HOP_LE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/admin/courses/5").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_HOP_LE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/courses/5").with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(service, never()).create(any());
        verify(service, never()).update(anyLong(), any());
        verify(service, never()).delete(anyLong());
    }

    @Test
    void hocSinhGoiBaThaoTacGhiDeuBi403VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(post("/api/admin/courses").with(user(hocSinh())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(put("/api/admin/courses/5").with(user(hocSinh())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/courses/5").with(user(hocSinh())).with(csrf()))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any());
        verify(service, never()).update(anyLong(), any());
        verify(service, never()).delete(anyLong());
    }

    @Test
    void adminThieuCsrfTokenBi403VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(post("/api/admin/courses").with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/courses/5").with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/courses/5").with(user(admin())))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any());
        verify(service, never()).update(anyLong(), any());
        verify(service, never()).delete(anyLong());
    }

    // ---- Tạo ----

    @Test
    void adminTaoKhoaThanhCongTra201VoiNamTruong() throws Exception {
        when(service.create(any())).thenReturn(khoa());

        mockMvc.perform(post("/api/admin/courses").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.slug").value("tin-hoc-10"))
                .andExpect(jsonPath("$.title").value("Tin học 10"))
                .andExpect(jsonPath("$.description").value("Chương trình lớp 10"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        ArgumentCaptor<AdminCourseRequest> sent = ArgumentCaptor.forClass(AdminCourseRequest.class);
        verify(service).create(sent.capture());
        assertThat(sent.getValue().slug()).isEqualTo("tin-hoc-10");
        assertThat(sent.getValue().sortOrder()).isEqualTo(1);
    }

    @Test
    void taoKhoaDuLieuSaiBi400KemDanhSachTruongLoiVaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(post("/api/admin/courses").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"Tin Hoc","title":"","sortOrder":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "slug", "title", "sortOrder")));

        verify(service, never()).create(any());
    }

    @Test
    void taoKhoaThanChuaPhaiJsonBi400() throws Exception {
        mockMvc.perform(post("/api/admin/courses").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("khong-phai-json"))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void taoKhoaTrungSlugBi409DangProblemJsonVaThongBaoKhongLapLaiSlug() throws Exception {
        when(service.create(any())).thenThrow(new CourseSlugAlreadyExistsException());

        mockMvc.perform(post("/api/admin/courses").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Slug khóa học đã tồn tại"))
                .andExpect(content().string(not(containsString("tin-hoc-10"))));
    }

    // ---- Sửa ----

    @Test
    void adminSuaKhoaThanhCongTra200() throws Exception {
        when(service.update(eq(5L), any())).thenReturn(khoa());

        mockMvc.perform(put("/api/admin/courses/5").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.slug").value("tin-hoc-10"));
    }

    @Test
    void suaKhoaDuLieuSaiBi400() throws Exception {
        mockMvc.perform(put("/api/admin/courses/5").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"slug":"ok","title":"x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sortOrder"));

        verify(service, never()).update(anyLong(), any());
    }

    @Test
    void suaKhoaKhongTonTaiBi404() throws Exception {
        when(service.update(eq(99L), any())).thenThrow(new CourseNotFoundException());

        mockMvc.perform(put("/api/admin/courses/99").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));
    }

    @Test
    void suaKhoaDoiSangSlugCuaKhoaKhacBi409() throws Exception {
        when(service.update(eq(5L), any())).thenThrow(new CourseSlugAlreadyExistsException());

        mockMvc.perform(put("/api/admin/courses/5").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Slug khóa học đã tồn tại"));
    }

    @Test
    void idKhongPhaiSoBi400ThongBaoCoDinhKhongLapLaiGiaTriVaServiceKhongDuocGoi() throws Exception {
        // detail là câu cố định. Trường instance (chuẩn RFC 9457) là đường dẫn của chính yêu cầu nên có "abc", không kiểm.
        mockMvc.perform(put("/api/admin/courses/abc").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Tham số trong đường dẫn không hợp lệ"));
        mockMvc.perform(delete("/api/admin/courses/abc").with(user(admin())).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tham số trong đường dẫn không hợp lệ"));

        verify(service, never()).update(anyLong(), any());
        verify(service, never()).delete(anyLong());
    }

    // ---- Xóa ----

    @Test
    void adminXoaKhoaThanhCongTra204KhongCoThan() throws Exception {
        mockMvc.perform(delete("/api/admin/courses/5").with(user(admin())).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(5L);
    }

    @Test
    void xoaKhoaKhongTonTaiBi404() throws Exception {
        doThrow(new CourseNotFoundException()).when(service).delete(99L);

        mockMvc.perform(delete("/api/admin/courses/99").with(user(admin())).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));
    }

    @Test
    void xoaKhoaConBaiBi409() throws Exception {
        doThrow(new CourseHasLessonsException()).when(service).delete(5L);

        mockMvc.perform(delete("/api/admin/courses/5").with(user(admin())).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Không xóa được khóa học còn bài học"));
    }

}
