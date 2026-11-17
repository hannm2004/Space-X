package vn.spacex.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import vn.spacex.user.User;

import java.io.IOException;

// Chạy sau khi Google xác thực xong (đã kiểm chữ ký ID token, state). Đổi "người dùng Google" thành người dùng của ứng
// dụng (AppUserDetails) rồi lưu vào session, để từ đây mọi thứ giống đăng nhập bằng mật khẩu: /api/me, phân quyền, CSRF.
@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleAccountService accountService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public GoogleLoginSuccessHandler(GoogleAccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            reject(request, response, GoogleLoginRejectedException.Reason.FAILED);
            return;
        }
        // Thiếu claim email_verified thì coi là chưa xác minh.
        GoogleProfile profile = new GoogleProfile(oidcUser.getSubject(), oidcUser.getEmail(),
                Boolean.TRUE.equals(oidcUser.getEmailVerified()), oidcUser.getFullName());
        User user;
        try {
            user = accountService.signIn(profile);
        } catch (GoogleLoginRejectedException e) {
            reject(request, response, e.reason());
            return;
        }

        AppUserDetails me = AppUserDetails.from(user);
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(me, null, me.getAuthorities()));
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        response.sendRedirect(request.getContextPath() + "/");
    }

    // Bộ lọc OAuth2 đã lưu đăng nhập Google vào session trước khi gọi handler này, nên khi từ chối phải hủy session
    // và xóa ngữ cảnh, nếu không người này vẫn đang "đăng nhập" bằng danh tính Google chưa được ứng dụng chấp nhận.
    private void reject(HttpServletRequest request, HttpServletResponse response,
                        GoogleLoginRejectedException.Reason reason) throws IOException {
        securityContextHolderStrategy.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/dang-nhap?loi=" + reason.code());
    }
}
