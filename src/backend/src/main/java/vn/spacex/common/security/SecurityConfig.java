package vn.spacex.common.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

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
                        // Chỉ đọc danh sách khóa và bài đã đăng là công khai, và chỉ bằng GET. "*" khớp đúng một đoạn
                        // đường dẫn nên /api/courses/{slug} (nếu sau này có) vẫn đòi đăng nhập.
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/*/lessons").permitAll()
                        // Spring lấy luật khớp đầu tiên nên luật Admin phải đứng trước "/api/**" (D26).
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        // Frontend (file tĩnh, trang React) được đóng chung vào JAR nên không chặn.
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint401())
                        .accessDeniedHandler(handler403()))
                // Mặc định Spring lưu yêu cầu bị chặn vào session để quay lại sau khi đăng nhập (dành cho web có trang
                // đăng nhập). API này chỉ trả 401 nên không cần; bỏ đi để yêu cầu ẩn danh không ghi dòng nào vào MySQL.
                .requestCache(cache -> cache.disable())
                // Đăng xuất: hủy session và trả 204. Bộ lọc này đòi POST kèm CSRF token như mọi thao tác ghi.
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                // CSRF luôn bật theo D24; không gọi csrf.disable(). Token đi bằng cookie XSRF-TOKEN và header X-XSRF-TOKEN
                // đúng như docs/API.md, để giao diện React (và curl) lấy được token mà không cần endpoint riêng.
                .csrf(csrf -> csrf
                        // withHttpOnlyFalse: JavaScript phải đọc được cookie để chép sang header. Cookie này chỉ chứa
                        // token CSRF, không phải session, nên lộ cho JavaScript trên cùng trang là chấp nhận được.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfTokenRequestHandler()));
        return http.build();
    }

    // Mặc định Spring chỉ nạp token khi có người đọc nên yêu cầu GET không nhận cookie. Đặt tên thuộc tính là null để
    // nạp token ở mọi yêu cầu, cookie luôn được phát. Bộ xử lý này nhận token thô từ header, không mã hóa XOR chống
    // BREACH (phiên bản XOR dành cho trang HTML có chèn token vào nội dung nén); API JSON này không chèn token vào thân.
    private static CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
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
