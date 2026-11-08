package vn.spacex.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
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
import vn.spacex.course.CourseHasLessonsException;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.course.CourseSlugAlreadyExistsException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.spacex.image.ImageNotFoundException;
import vn.spacex.image.ImageService;
import vn.spacex.image.ImageTooLargeException;
import vn.spacex.image.UnsupportedImageTypeException;
import vn.spacex.lesson.LessonNotFoundException;
import vn.spacex.lesson.LessonSlugAlreadyExistsException;
import vn.spacex.user.NoPasswordAccountException;
import vn.spacex.user.WrongCurrentPasswordException;

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

    // Câu cố định, không lặp lại slug người dùng gửi lên.
    @ExceptionHandler(CourseNotFoundException.class)
    ProblemDetail xuLyKhongTimThayKhoaHoc() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học");
    }

    @ExceptionHandler(CourseSlugAlreadyExistsException.class)
    ProblemDetail xuLySlugKhoaHocTrung() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Slug khóa học đã tồn tại");
    }

    @ExceptionHandler(CourseHasLessonsException.class)
    ProblemDetail xuLyKhoaHocConBai() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Không xóa được khóa học còn bài học");
    }

    @ExceptionHandler(LessonSlugAlreadyExistsException.class)
    ProblemDetail xuLySlugBaiHocTrung() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Slug bài học đã tồn tại");
    }

    // Cùng một câu cho bài không tồn tại và bài nháp, và không lặp lại slug người dùng gửi lên.
    @ExceptionHandler(LessonNotFoundException.class)
    ProblemDetail xuLyKhongTimThayBaiHoc() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy bài học");
    }

    // Đã đăng nhập nên báo rõ "mật khẩu hiện tại sai" không làm lộ gì thêm, khác với lúc đăng nhập (D25).
    @ExceptionHandler(WrongCurrentPasswordException.class)
    ProblemDetail xuLySaiMatKhauCu() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
    }

    @ExceptionHandler(NoPasswordAccountException.class)
    ProblemDetail xuLyTaiKhoanKhongCoMatKhau() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Tài khoản đăng nhập bằng Google không có mật khẩu để đổi");
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

    @ExceptionHandler(UnsupportedImageTypeException.class)
    ProblemDetail xuLyAnhKhongHopLe() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Chỉ nhận ảnh PNG, JPEG hoặc WebP hợp lệ");
    }

    @ExceptionHandler(ImageTooLargeException.class)
    ProblemDetail xuLyAnhQuaLon() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, ImageService.TOO_LARGE_DETAIL);
    }

    // Cùng một câu cho ảnh không tồn tại và ảnh của bài nháp.
    @ExceptionHandler(ImageNotFoundException.class)
    ProblemDetail xuLyKhongTimThayAnh() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh");
    }

    // Vượt giới hạn của spring.servlet.multipart (tầng container, trước khi tới controller).
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
                ImageService.TOO_LARGE_DETAIL);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // Ví dụ id không phải số. Câu mặc định của Spring lặp lại giá trị người dùng gửi lên nên thay bằng câu cố định.
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Tham số trong đường dẫn không hợp lệ");
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
