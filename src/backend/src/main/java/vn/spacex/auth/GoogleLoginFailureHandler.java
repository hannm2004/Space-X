package vn.spacex.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Google báo lỗi hoặc người dùng bấm Hủy ở màn hình xin quyền. Chỉ đưa mã ngắn lên địa chỉ; chi tiết lỗi của Google
// (có thể chứa thông tin kỹ thuật nhạy cảm) không bao giờ đến trình duyệt.
@Component
public class GoogleLoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginFailureHandler.class);

    static final String CANCELLED_CODE = "google-huy";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        boolean cancelled = exception instanceof OAuth2AuthenticationException oauth
                && "access_denied".equals(oauth.getError().getErrorCode());
        // Chỉ ghi loại lỗi và mã lỗi OAuth2 (ví dụ invalid_client, authorization_request_not_found), không ghi thông điệp
        // chi tiết của Google, để còn biết vì sao thất bại mà không lộ gì ra trình duyệt.
        if (!cancelled) {
            String oauthCode = exception instanceof OAuth2AuthenticationException oauth
                    ? oauth.getError().getErrorCode() : "-";
            log.warn("Dang nhap Google that bai: {} (ma loi OAuth2: {})", exception.getClass().getSimpleName(), oauthCode);
        }
        String code = cancelled ? CANCELLED_CODE : GoogleLoginRejectedException.Reason.FAILED.code();
        response.sendRedirect(request.getContextPath() + "/dang-nhap?loi=" + code);
    }
}
