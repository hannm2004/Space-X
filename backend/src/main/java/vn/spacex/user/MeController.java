package vn.spacex.user;

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

// Người dùng hiện tại lấy từ session (@AuthenticationPrincipal), không nhận userId từ trình duyệt.
// SecurityConfig đã đòi đăng nhập cho mọi /api/** nên ở đây không bao giờ có người dùng ẩn danh.
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
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
    void changePassword(@AuthenticationPrincipal AppUserDetails me, @Valid @RequestBody ChangePasswordRequest body) {
        meService.changePassword(me.id(), body);
    }
}
