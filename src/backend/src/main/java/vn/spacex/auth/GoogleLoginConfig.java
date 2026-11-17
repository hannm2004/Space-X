package vn.spacex.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

// Đăng nhập Google chỉ bật khi có GOOGLE_CLIENT_ID. Không có thì ứng dụng chạy bình thường (máy chưa cấu hình, Render
// trước khi đặt biến), không có đường dẫn /oauth2/... nào. Không đặt giá trị mặc định cho client-id/secret trong code.
@Configuration
public class GoogleLoginConfig {

    private static final String ENABLED = "!'${app.google.client-id:}'.isBlank()";

    // Chỗ SecurityConfig lấy hai bộ xử lý kết quả; chỉ có bean này khi tính năng bật.
    public record GoogleLoginHandlers(GoogleLoginSuccessHandler success, GoogleLoginFailureHandler failure) {
    }

    // Google có sẵn mẫu trong Spring (địa chỉ xác thực, token, khóa công khai...). Chỉ cần id và secret của ứng dụng.
    // Phạm vi: openid (ID token), email, profile (tên). Không xin quyền đọc Gmail, Drive hay danh bạ.
    @Bean
    @ConditionalOnExpression(ENABLED)
    ClientRegistrationRepository googleClientRegistrationRepository(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret:}") String clientSecret) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "email", "profile")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    @Bean
    @ConditionalOnExpression(ENABLED)
    GoogleLoginHandlers googleLoginHandlers(GoogleLoginSuccessHandler success, GoogleLoginFailureHandler failure) {
        return new GoogleLoginHandlers(success, failure);
    }
}
