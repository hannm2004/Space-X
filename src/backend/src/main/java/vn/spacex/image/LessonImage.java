package vn.spacex.image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Ảnh lưu thẳng trong MySQL (Render Free không giữ tệp trên đĩa). Danh sách ảnh dùng LessonImageInfo để không tải `data`.
@Entity
@Table(name = "lesson_images")
public class LessonImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false, updatable = false)
    private Long lessonId;

    // Chỉ để hiển thị: tên đã được làm sạch, không bao giờ dùng làm đường dẫn tệp.
    @Column(name = "file_name", nullable = false)
    private String fileName;

    // Loại do ứng dụng nhận diện từ nội dung, không phải loại trình duyệt khai báo.
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    // Cột MEDIUMBLOB. Cần khai báo kiểu để Hibernate (ddl-auto=validate) coi nó khớp thay vì VARBINARY mặc định.
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(nullable = false)
    private byte[] data;

    protected LessonImage() {
    }

    public static LessonImage create(Long lessonId, String fileName, String contentType, byte[] data) {
        LessonImage image = new LessonImage();
        image.lessonId = lessonId;
        image.fileName = fileName;
        image.contentType = contentType;
        image.sizeBytes = data.length;
        image.data = data;
        return image;
    }

    public Long getId() {
        return id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public String getFileName() {
        return fileName;
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
}
