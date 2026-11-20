package vn.spacex.image;

import java.time.LocalDateTime;

// Cặp (khóa, thời điểm đặt ảnh bìa) cho truy vấn gom nhiều khóa một lần, không tải dữ liệu ảnh.
public interface CourseCoverVersion {

    Long getCourseId();

    LocalDateTime getUpdatedAt();
}
