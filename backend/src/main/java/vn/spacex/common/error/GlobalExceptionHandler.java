package vn.spacex.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Lưới an toàn cuối cùng: chi tiết lỗi chỉ ghi vào log ở server, người dùng chỉ thấy câu chung.
    @ExceptionHandler(Exception.class)
    ProblemDetail xuLyLoiBatNgo(Exception ex, HttpServletRequest request) {
        log.error("Loi khong xu ly duoc tai {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Có lỗi xảy ra phía máy chủ. Vui lòng thử lại sau.");
    }
}
