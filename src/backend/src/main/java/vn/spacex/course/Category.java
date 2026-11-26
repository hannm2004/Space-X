package vn.spacex.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Nhóm chương trình. Hiện mới cần id để gắn khóa vào nhóm; tên và thứ tự sẽ ánh xạ khi có API danh sách theo nhóm.
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String slug;

    protected Category() {
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }
}
