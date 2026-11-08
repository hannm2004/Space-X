package vn.spacex.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

// Cùng bảng lessons với Lesson nhưng có thêm content_md (MEDIUMTEXT). Tách riêng để danh sách bài (Lesson) không bao giờ
// tải nội dung; chỉ trang đọc một bài mới dùng lớp này. @Immutable: Hibernate không bao giờ ghi lớp này xuống database.
@Entity
@Immutable
@Table(name = "lessons")
public class LessonContent {

    @Id
    private Long id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    private String summary;

    @Column(name = "content_md", nullable = false)
    private String contentMd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonStatus status;

    protected LessonContent() {
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContentMd() {
        return contentMd;
    }

    public LessonStatus getStatus() {
        return status;
    }
}
