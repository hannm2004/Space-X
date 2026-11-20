package vn.spacex.image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// Ảnh bìa của một khóa học, lưu trong MySQL như ảnh bài học (Render Free không giữ tệp trên đĩa). Khóa chính là mã khóa:
// mỗi khóa tối đa một ảnh, đặt ảnh mới là ghi đè dòng cũ. Danh sách khóa dùng CourseCoverVersion để không tải `data`.
@Entity
@Table(name = "course_covers")
public class CourseCover {

    @Id
    @Column(name = "course_id")
    private Long courseId;

    // Loại do ứng dụng nhận diện từ nội dung, không phải loại trình duyệt khai báo.
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    // Cột MEDIUMBLOB: khai báo kiểu để Hibernate (ddl-auto=validate) coi nó khớp thay vì VARBINARY mặc định.
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(nullable = false)
    private byte[] data;

    // Dùng làm mốc phiên bản trong đường dẫn (?v=) để trình duyệt tải lại khi ảnh bìa đổi.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CourseCover() {
    }

    public static CourseCover create(Long courseId, String contentType, byte[] data, LocalDateTime updatedAt) {
        CourseCover cover = new CourseCover();
        cover.courseId = courseId;
        cover.contentType = contentType;
        cover.sizeBytes = data.length;
        cover.data = data;
        cover.updatedAt = updatedAt;
        return cover;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getData() {
        return data;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
