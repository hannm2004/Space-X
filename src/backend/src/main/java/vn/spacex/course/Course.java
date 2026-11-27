package vn.spacex.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    // Có thể NULL: khóa mới tạo chưa cần mô tả.
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    // Mã khóa dùng để sinh mã lớp: đặt một lần, đổi slug không đổi mã.
    @Column(nullable = false)
    private String code;

    // Giá trị ban đầu phải đặt ở đây: Hibernate luôn ghi cả hai cột nên DEFAULT của database không được dùng.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    // Đồng Việt Nam, số nguyên; 0 là chưa có học phí.
    @Column(name = "tuition_vnd", nullable = false)
    private long tuitionVnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseLevel level = CourseLevel.BASIC;

    // Cột là SMALLINT; khai báo kiểu để Hibernate (ddl-auto=validate) không đòi INTEGER.
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "total_hours", nullable = false)
    private int totalHours;

    protected Course() {
    }

    public void changeLevel(CourseLevel level) {
        this.level = level;
    }

    public void changeTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }

    // Chuyển nhóm không đổi mã khóa.
    public void moveTo(Long categoryId) {
        this.categoryId = categoryId;
    }

    public CourseLevel getLevel() {
        return level;
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void changeStatus(CourseStatus status) {
        this.status = status;
    }

    public void changeTuition(long tuitionVnd) {
        this.tuitionVnd = tuitionVnd;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public long getTuitionVnd() {
        return tuitionVnd;
    }

    public void placeIn(Long categoryId, String code) {
        this.categoryId = categoryId;
        this.code = code;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCode() {
        return code;
    }

    public static Course create(String slug, String title, String description, int sortOrder) {
        Course course = new Course();
        course.update(slug, title, description, sortOrder);
        return course;
    }

    public void update(String slug, String title, String description, int sortOrder) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
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

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
