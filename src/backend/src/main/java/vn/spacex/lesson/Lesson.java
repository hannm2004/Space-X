package vn.spacex.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Chỉ ánh xạ các cột cần cho danh sách bài (4.1). Cố ý chưa ánh xạ content_md (MEDIUMTEXT) và created_by:
// nhờ vậy truy vấn danh sách không bao giờ tải nội dung bài. Hai cột này được thêm ở 4.2 và 4.4.
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Giữ id thay vì quan hệ @ManyToOne: danh sách bài không cần nạp cả khóa học.
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonStatus status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Lesson() {
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
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

    public LessonStatus getStatus() {
        return status;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
