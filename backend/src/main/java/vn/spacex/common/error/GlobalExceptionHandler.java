package vn.spacex.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import vn.spacex.auth.EmailAlreadyExistsException;
import vn.spacex.common.security.SecurityConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail xuLyEmailTrung() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email đã được đăng ký");
    }

    // Lỗi phân quyền ném từ trong controller (ví dụ @PreAuthorize sau này). Nếu không có handler này thì
    // handler Exception bên dưới sẽ đổi nó thành 500. Không đưa ex.getMessage() vào phản hồi.
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail xuLyKhongCoQuyen() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, SecurityConfig.FORBIDDEN_DETAIL);
    }

    // Thêm danh sách trường lỗi (docs/API.md). Chỉ trả tên trường và thông báo, không trả giá trị người dùng gửi
    // lên để mật khẩu không bị phản hồi lại.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .sorted(Comparator.comparing((Map<String, String> e) -> e.get("field")).thenComparing(e -> e.get("message")))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // Lưới an toàn cuối cùng: chi tiết lỗi chỉ ghi vào log ở server, người dùng chỉ thấy câu chung.
    @ExceptionHandler(Exception.class)
    ProblemDetail xuLyLoiBatNgo(Exception ex, HttpServletRequest request) {
        log.error("Loi khong xu ly duoc tai {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Có lỗi xảy ra phía máy chủ. Vui lòng thử lại sau.");
    }
}
