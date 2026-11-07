package vn.spacex.common.security;

import org.springframework.boot.session.autoconfigure.DefaultCookieSerializerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfig {

    // Spring Boot 4 + Spring Session mặc định phát cookie SESSION chỉ có "Path=/", không HttpOnly, không SameSite
    // (các thuộc tính server.servlet.session.cookie.* không áp dụng cho cookie này). Đặt tường minh:
    // - HttpOnly: JavaScript không đọc được cookie, nên XSS không lấy cắp được phiên đăng nhập.
    // - SameSite=Lax: trình duyệt không gửi cookie khi trang khác gửi POST tới, lớp phòng thủ thứ hai ngoài CSRF token.
    // Cờ Secure để 3.8: cần Render báo đúng là HTTPS (forward-headers) rồi mới bật được an toàn.
    @Bean
    DefaultCookieSerializerCustomizer sessionCookieCustomizer() {
        return serializer -> {
            serializer.setUseHttpOnlyCookie(true);
            serializer.setSameSite("Lax");
        };
    }
}
