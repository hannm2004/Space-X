package vn.spacex.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderBySortOrderAscIdAsc();

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Dùng khi sửa: slug hiện tại của chính khóa đó không bị tính là trùng.
    boolean existsBySlugAndIdNot(String slug, Long id);
}
