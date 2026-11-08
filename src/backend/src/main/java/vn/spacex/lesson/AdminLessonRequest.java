package vn.spacex.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// Dùng cho cả tạo và sửa. Không có trạng thái và người tạo: bài mới luôn là nháp, người tạo lấy từ session.
public record AdminLessonRequest(
        @NotNull(message = "Khóa học không được để trống")
        @Positive(message = "Mã khóa học không hợp lệ")
        Long courseId,

        // Chữ thường, số, gạch ngang nối giữa các từ: dùng thẳng làm đường dẫn nên không có dấu cách hay chữ hoa.
        @NotBlank(message = "Slug không được để trống")
        @Size(max = 150, message = "Slug tối đa 150 ký tự")
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                message = "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang giữa các từ")
        String slug,

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự")
        String summary,

        // Có thể rỗng (bài nháp mới tạo) nhưng không được thiếu. Giới hạn để một yêu cầu không ngốn bộ nhớ.
        @NotNull(message = "Nội dung không được thiếu")
        @Size(max = 100_000, message = "Nội dung tối đa 100000 ký tự")
        String contentMd,

        @NotNull(message = "Thứ tự không được để trống")
        @PositiveOrZero(message = "Thứ tự không được âm")
        Integer sortOrder) {

    // Chuẩn hóa trước khi kiểm tra. Nội dung Markdown giữ nguyên (khoảng trắng đầu dòng có nghĩa trong code).
    public AdminLessonRequest {
        slug = slug == null ? null : slug.trim();
        title = title == null ? null : title.trim();
        summary = summary == null || summary.isBlank() ? null : summary.trim();
    }
}
