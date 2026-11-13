package vn.spacex.auth;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

// Hủy các session khác của một tài khoản (D28). Session trong MySQL được đánh chỉ mục theo PRINCIPAL_NAME, chính là email
// (V2), và email không đổi được nên tìm theo email là đủ.
@Component
public class OtherSessionsRevoker {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public OtherSessionsRevoker(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    // Dùng khi admin khóa tài khoản hoặc đặt lại mật khẩu: người bị tác động không gọi API nên không có session hiện tại để giữ.
    public void revokeAll(String email) {
        for (String sessionId : sessionRepository.findByPrincipalName(email).keySet()) {
            sessionRepository.deleteById(sessionId);
        }
    }

    public void revokeOthers(String email, String currentSessionId) {
        for (String sessionId : sessionRepository.findByPrincipalName(email).keySet()) {
            if (!sessionId.equals(currentSessionId)) {
                sessionRepository.deleteById(sessionId);
            }
        }
    }
}
