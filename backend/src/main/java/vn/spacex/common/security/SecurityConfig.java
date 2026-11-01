package vn.spacex.common.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    // Dùng chung cho 403 do bộ lọc (ở đây) và 403 do controller ném AccessDeniedException (GlobalExceptionHandler).
    public static final String FORBIDDEN_DETAIL = "Bạn không có quyền thực hiện thao tác này";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login").permitAll()
                        // Spring lấy luật khớp đầu tiên nên luật Admin phải đứng trước "/api/**" (D26).
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        // Frontend (file tĩnh, trang React) được đóng chung vào JAR nên không chặn.
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint401())
                        .accessDeniedHandler(handler403()))
                // Đăng xuất: hủy session và trả 204. Bộ lọc này đòi POST kèm CSRF token như mọi thao tác ghi.
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                // CSRF để mặc định (bật) theo D24; không gọi csrf.disable().
                .csrf(Customizer.withDefaults());
        return http.build();
    }

    // BCrypt tự sinh salt và lưu cùng chuỗi băm (60 ký tự, vừa cột password_hash).
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Không đăng nhập mà vào API thì trả 401 dạng problem+json, giống các lỗi khác của dự án.
    private AuthenticationEntryPoint entryPoint401() {
        return (request, response, authException) -> writeProblem(
                response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                "Bạn cần đăng nhập để thực hiện thao tác này");
    }

    // Đã đăng nhập nhưng thiếu quyền (hoặc thiếu CSRF token) thì trả 403 dạng problem+json. Bộ lọc ném lỗi này
    // trước khi tới controller nên @RestControllerAdvice không bắt được, phải xử lý ở đây.
    private AccessDeniedHandler handler403() {
        return (request, response, accessDeniedException) -> writeProblem(
                response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", FORBIDDEN_DETAIL);
    }

    // Nội dung cố định, không đưa dữ liệu người dùng vào nên không cần thư viện JSON.
    private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s"}"""
                .formatted(title, status, detail));
    }
}
