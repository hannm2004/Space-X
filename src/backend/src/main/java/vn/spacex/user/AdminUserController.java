package vn.spacex.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.auth.OtherSessionsRevoker;
import vn.spacex.common.api.PageResponse;

// Chỉ ADMIN gọi được: luật "/api/admin/**" trong SecurityConfig. Người thực hiện lấy từ session (@AuthenticationPrincipal).
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final OtherSessionsRevoker otherSessionsRevoker;

    public AdminUserController(AdminUserService adminUserService, OtherSessionsRevoker otherSessionsRevoker) {
        this.adminUserService = adminUserService;
        this.otherSessionsRevoker = otherSessionsRevoker;
    }

    @GetMapping
    PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return adminUserService.list(q, page, size);
    }

    @PatchMapping("/{id}/status")
    AdminUserResponse changeStatus(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id,
                                   @Valid @RequestBody AdminUserStatusRequest request) {
        AdminUserResponse response = adminUserService.changeEnabled(me.id(), id, request.enabled());
        // Session đã tạo trước khi bị khóa vẫn còn trong database và vẫn dùng được nếu không xóa. Chỉ chạy khi khóa
        // thành công (lỗi ở dòng trên ném ngoại lệ) và sau khi transaction của service đã commit.
        if (!response.enabled()) {
            otherSessionsRevoker.revokeAll(response.email());
        }
        return response;
    }

    @PostMapping("/{id}/reset-password")
    ResponseEntity<ResetPasswordResponse> resetPassword(@AuthenticationPrincipal AppUserDetails me,
                                                        @PathVariable Long id) {
        ResetPasswordResponse response = adminUserService.resetPassword(me.id(), id);
        // Mật khẩu cũ không còn đúng thì mọi thiết bị đang đăng nhập bằng nó phải đăng nhập lại.
        otherSessionsRevoker.revokeAll(response.email());
        // Phản hồi chứa mật khẩu nên cấm trình duyệt và proxy lưu lại.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
