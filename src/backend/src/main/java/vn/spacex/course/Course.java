package vn.spacex.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    protected Course() {
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
