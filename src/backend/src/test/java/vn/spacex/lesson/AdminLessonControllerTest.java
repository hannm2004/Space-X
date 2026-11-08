package vn.spacex.lesson;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.common.api.PageResponse;
import vn.spacex.common.error.GlobalExceptionHandler;
import vn.spacex.common.security.SecurityConfig;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.user.Role;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLessonController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminLessonControllerTest {

    private static final long ADMIN_ID = 1L;

    private static final String BODY_HOP_LE = """
            {"courseId":2,"slug":"may-tinh-la-gi","title":"Máy tính là gì","summary":"Tóm tắt",
             "contentMd":"# Nội dung","sortOrder":3}""";

    private static final String THONG_BAO_THAM_SO_SAI = "Tham số trong đường dẫn không hợp lệ";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminLessonService service;

    private static AppUserDetails admin() {
        return new AppUserDetails(ADMIN_ID, "admin@example.com", null, "Quản trị viên", Role.ADMIN, true);
    }

    private static AppUserDetails hocSinh() {
        return new AppUserDetails(7L, "hs@example.com", null, "Học sinh", Role.LEARNER, true);
    }

    private static AdminLessonResponse bai() {
        return new AdminLessonResponse(11L, 2L, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt", "# Nội dung",
                LessonStatus.DRAFT, 3);
    }

    // ---- Phân quyền và CSRF ----

    @Test
    void khachChuaDangNhapGoiMoiThaoTacBi401VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(get("/api/admin/lessons")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/lessons/11")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/lessons").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(BODY_HOP_LE)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/admin/lessons/11").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(BODY_HOP_LE)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/lessons/11/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/lessons/11").with(csrf())).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void hocSinhGoiMoiThaoTacKeCaDocBi403VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(get("/api/admin/lessons").with(user(hocSinh())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/admin/lessons/11").with(user(hocSinh()))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/lessons").with(user(hocSinh())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/lessons/11").with(user(hocSinh())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/lessons/11/status").with(user(hocSinh())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/lessons/11").with(user(hocSinh())).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminThieuCsrfTokenBi403ODaThaoTacGhi() throws Exception {
        mockMvc.perform(post("/api/admin/lessons").with(user(admin()))
                .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/lessons/11").with(user(admin()))
                .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/lessons/11/status").with(user(admin()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/lessons/11").with(user(admin()))).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // ---- Danh sách ----

    @Test
    void danhSachKhongThamSoDungMacDinhTrang0Kich20KhongLoc() throws Exception {
        when(service.list(null, null, 0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/admin/lessons").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(0));

        verify(service).list(null, null, 0, 20);
    }

    @Test
    void danhSachTruyenDungBoLocVaTrangVaMoiBaiChiCoBayTruongKhongCoNoiDung() throws Exception {
        when(service.list(3L, LessonStatus.PUBLISHED, 2, 10)).thenReturn(new PageResponse<>(
                List.of(new AdminLessonSummaryResponse(11L, 3L, "bai-1", "Bài 1", null, LessonStatus.PUBLISHED, 4)),
                2, 10, 21));

        mockMvc.perform(get("/api/admin/lessons?courseId=3&status=PUBLISHED&page=2&size=10").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(21))
                .andExpect(jsonPath("$.items[0].*", hasSize(7)))
                .andExpect(jsonPath("$.items[0].id").value(11))
                .andExpect(jsonPath("$.items[0].courseId").value(3))
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].sortOrder").value(4))
                .andExpect(jsonPath("$.items[0].contentMd").doesNotExist());
    }

    @Test
    void kichThuocTrangToiDa50VaToiThieu1() throws Exception {
        when(service.list(null, null, 0, 50)).thenReturn(new PageResponse<>(List.of(), 0, 50, 0));

        mockMvc.perform(get("/api/admin/lessons?size=50").with(user(admin()))).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/lessons?size=51").with(user(admin()))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/lessons?size=0").with(user(admin()))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/lessons?page=-1").with(user(admin()))).andExpect(status().isBadRequest());

        verify(service, never()).list(any(), any(), eq(0), eq(51));
        verify(service, never()).list(any(), any(), eq(0), eq(0));
    }

    @Test
    void boLocSaiKieuBi400ThongBaoCoDinhKhongLapLaiGiaTri() throws Exception {
        mockMvc.perform(get("/api/admin/lessons?status=KHONG-CO").with(user(admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(THONG_BAO_THAM_SO_SAI));
        mockMvc.perform(get("/api/admin/lessons?courseId=abc").with(user(admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(THONG_BAO_THAM_SO_SAI));

        verifyNoInteractions(service);
    }

    // ---- Chi tiết ----

    @Test
    void adminXemChiTietBaiNhapCoDuTamTruongKemNoiDung() throws Exception {
        when(service.get(11L)).thenReturn(bai());

        mockMvc.perform(get("/api/admin/lessons/11").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(8)))
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.courseId").value(2))
                .andExpect(jsonPath("$.slug").value("may-tinh-la-gi"))
                .andExpect(jsonPath("$.contentMd").value("# Nội dung"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.sortOrder").value(3))
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void xemChiTietBaiKhongTonTaiBi404() throws Exception {
        when(service.get(99L)).thenThrow(new LessonNotFoundException());

        mockMvc.perform(get("/api/admin/lessons/99").with(user(admin())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy bài học"));
    }

    // ---- Tạo ----

    @Test
    void taoBaiTra201VaNguoiTaoLayTuSessionKhongTinBodyGuiLen() throws Exception {
        when(service.create(eq(ADMIN_ID), any())).thenReturn(bai());

        mockMvc.perform(post("/api/admin/lessons").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":2,"slug":"may-tinh-la-gi","title":"Máy tính là gì","contentMd":"x",
                                 "sortOrder":3,"createdBy":999,"status":"PUBLISHED"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.id").value(11));

        ArgumentCaptor<AdminLessonRequest> sent = ArgumentCaptor.forClass(AdminLessonRequest.class);
        verify(service).create(eq(ADMIN_ID), sent.capture());
        assertThat(sent.getValue().courseId()).isEqualTo(2L);
        assertThat(sent.getValue().slug()).isEqualTo("may-tinh-la-gi");
        verify(service, never()).create(eq(999L), any());
    }

    @Test
    void taoBaiDuLieuSaiBi400KemDanhSachTruongLoi() throws Exception {
        mockMvc.perform(post("/api/admin/lessons").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":0,"slug":"Bai Mot","title":"","contentMd":null,"sortOrder":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "courseId", "slug", "title", "contentMd", "sortOrder")));

        verifyNoInteractions(service);
    }

    @Test
    void taoBaiThanKhongPhaiJsonBi400() throws Exception {
        mockMvc.perform(post("/api/admin/lessons").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("khong-phai-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void taoBaiVaoKhoaKhongTonTaiBi404() throws Exception {
        when(service.create(eq(ADMIN_ID), any())).thenThrow(new CourseNotFoundException());

        mockMvc.perform(post("/api/admin/lessons").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));
    }

    @Test
    void taoBaiTrungSlugBi409() throws Exception {
        when(service.create(eq(ADMIN_ID), any())).thenThrow(new LessonSlugAlreadyExistsException());

        mockMvc.perform(post("/api/admin/lessons").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Slug bài học đã tồn tại"));
    }

    // ---- Sửa ----

    @Test
    void suaBaiThanhCongTra200() throws Exception {
        when(service.update(eq(11L), any())).thenReturn(bai());

        mockMvc.perform(put("/api/admin/lessons/11").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void suaBaiDuLieuSaiBi400() throws Exception {
        mockMvc.perform(put("/api/admin/lessons/11").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"courseId":2,"slug":"ok","title":"x","contentMd":"n"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sortOrder"));

        verify(service, never()).update(anyLong(), any());
    }

    @Test
    void suaBaiKhongTonTaiBi404KhoaKhongTonTaiBi404VaTrungSlugBi409() throws Exception {
        when(service.update(eq(99L), any())).thenThrow(new LessonNotFoundException());
        when(service.update(eq(12L), any())).thenThrow(new CourseNotFoundException());
        when(service.update(eq(13L), any())).thenThrow(new LessonSlugAlreadyExistsException());

        mockMvc.perform(put("/api/admin/lessons/99").with(user(admin())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy bài học"));
        mockMvc.perform(put("/api/admin/lessons/12").with(user(admin())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy khóa học"));
        mockMvc.perform(put("/api/admin/lessons/13").with(user(admin())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isConflict());
    }

    // ---- Đổi trạng thái ----

    @Test
    void dangBaiTra200VoiTrangThaiMoi() throws Exception {
        when(service.changeStatus(11L, LessonStatus.PUBLISHED)).thenReturn(new AdminLessonResponse(
                11L, 2L, "may-tinh-la-gi", "Máy tính là gì", "Tóm tắt", "# Nội dung", LessonStatus.PUBLISHED, 3));

        mockMvc.perform(patch("/api/admin/lessons/11/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void doiTrangThaiSaiGiaTriHoacThieuBi400VaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(patch("/api/admin/lessons/11/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/admin/lessons/11/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verifyNoInteractions(service);
    }

    @Test
    void doiTrangThaiBaiKhongTonTaiBi404() throws Exception {
        when(service.changeStatus(99L, LessonStatus.PUBLISHED)).thenThrow(new LessonNotFoundException());

        mockMvc.perform(patch("/api/admin/lessons/99/status").with(user(admin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- Xóa ----

    @Test
    void xoaBaiTra204KhongCoThan() throws Exception {
        mockMvc.perform(delete("/api/admin/lessons/11").with(user(admin())).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(11L);
    }

    @Test
    void xoaBaiKhongTonTaiBi404() throws Exception {
        doThrow(new LessonNotFoundException()).when(service).delete(99L);

        mockMvc.perform(delete("/api/admin/lessons/99").with(user(admin())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void idKhongPhaiSoBi400ThongBaoCoDinhVaServiceKhongDuocGoi() throws Exception {
        mockMvc.perform(get("/api/admin/lessons/abc").with(user(admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(THONG_BAO_THAM_SO_SAI));
        mockMvc.perform(delete("/api/admin/lessons/abc").with(user(admin())).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
