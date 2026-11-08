package vn.spacex.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Bản đọc và ghi đầy đủ của bảng lessons, chỉ API Admin dùng. Tách khỏi Lesson (danh sách, không có nội dung) và LessonContent
// (trang đọc, chỉ-đọc) để hai đường đọc công khai vẫn không bao giờ tải hay ghi nhầm cột khác.
@Entity
@Table(name = "lessons")
public class AdminLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // Người tạo bài: lấy từ session lúc tạo và không bao giờ đổi sau đó.
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

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

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected AdminLesson() {
    }

    // Bài mới luôn bắt đầu là nháp: muốn học sinh thấy thì Admin phải đăng một cách chủ động.
    public static AdminLesson create(Long courseId, Long createdBy, String slug, String title, String summary,
                                     String contentMd, int sortOrder) {
        AdminLesson lesson = new AdminLesson();
        lesson.createdBy = createdBy;
        lesson.status = LessonStatus.DRAFT;
        lesson.update(courseId, slug, title, summary, contentMd, sortOrder);
        return lesson;
    }

    // Không đổi trạng thái và người tạo: trạng thái đổi riêng bằng changeStatus.
    public void update(Long courseId, String slug, String title, String summary, String contentMd, int sortOrder) {
        this.courseId = courseId;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.contentMd = contentMd;
        this.sortOrder = sortOrder;
    }

    public void changeStatus(LessonStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getCreatedBy() {
        return createdBy;
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

    public int getSortOrder() {
        return sortOrder;
    }
}
