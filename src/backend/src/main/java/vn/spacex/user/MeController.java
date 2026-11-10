package vn.spacex.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.spacex.auth.AppUserDetails;
import vn.spacex.auth.OtherSessionsRevoker;

// Người dùng hiện tại lấy từ session (@AuthenticationPrincipal), không nhận userId từ trình duyệt.
// SecurityConfig đã đòi đăng nhập cho mọi /api/** nên ở đây không bao giờ có người dùng ẩn danh.
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;
    private final OtherSessionsRevoker otherSessionsRevoker;

    public MeController(MeService meService, OtherSessionsRevoker otherSessionsRevoker) {
        this.meService = meService;
        this.otherSessionsRevoker = otherSessionsRevoker;
    }

    @GetMapping
    MeResponse get(@AuthenticationPrincipal AppUserDetails me) {
        return meService.getProfile(me.id());
    }

    @PutMapping
    MeResponse update(@AuthenticationPrincipal AppUserDetails me, @Valid @RequestBody UpdateProfileRequest body) {
        return meService.updateProfile(me.id(), body);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal AppUserDetails me, HttpSession session,
                        @Valid @RequestBody ChangePasswordRequest body) {
        meService.changePassword(me.id(), body);
        // Chỉ chạy khi đổi thành công (lỗi ở dòng trên ném ngoại lệ) và sau khi transaction của service đã commit.
        otherSessionsRevoker.revokeOthers(me.email(), session.getId());
    }
}
