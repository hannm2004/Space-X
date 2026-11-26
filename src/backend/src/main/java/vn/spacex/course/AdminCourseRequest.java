package vn.spacex.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCourseRequest(
        // Chữ thường, số, gạch ngang nối giữa các từ: dùng thẳng làm đường dẫn nên không có dấu cách hay chữ hoa.
        @NotBlank(message = "Slug không được để trống")
        @Size(max = 100, message = "Slug tối đa 100 ký tự")
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                message = "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang giữa các từ")
        String slug,

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description,

        @NotNull(message = "Thứ tự không được để trống")
        @PositiveOrZero(message = "Thứ tự không được âm")
        Integer sortOrder,

        // Hai trường dưới có thể bỏ trống: khi sửa thì giữ giá trị cũ (không vô tình gỡ khóa đang đăng),
        // khi tạo thì là bản nháp chưa có học phí.
        CourseStatus status,

        @PositiveOrZero(message = "Học phí không được âm")
        // Trần để chặn gõ thừa số 0; không phải quy định của trung tâm.
        @Max(value = 1_000_000_000L, message = "Học phí tối đa 1.000.000.000 đồng")
        Long tuitionVnd) {

    // Chuẩn hóa trước khi kiểm tra. Mô tả chỉ có khoảng trắng được lưu thành NULL như khi để trống.
    public AdminCourseRequest {
        slug = slug == null ? null : slug.trim();
        title = title == null ? null : title.trim();
        description = description == null || description.isBlank() ? null : description.trim();
    }
}
