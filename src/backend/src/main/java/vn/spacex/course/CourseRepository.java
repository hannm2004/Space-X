package vn.spacex.course;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderBySortOrderAscIdAsc();

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByCode(String code);

    // Danh sách công khai: chỉ khóa đã đăng. Tham số null nghĩa là không lọc theo trường đó.
    @Query("""
            select c from Course c
            where c.status = vn.spacex.course.CourseStatus.PUBLISHED
              and (:categorySlug is null
                   or c.categoryId in (select g.id from Category g where g.slug = :categorySlug))
              and (:level is null or c.level = :level)
              and c.totalHours between :minHours and :maxHours
              and c.tuitionVnd between :minTuition and :maxTuition
            """)
    List<Course> searchPublished(@Param("categorySlug") String categorySlug, @Param("level") CourseLevel level,
                                 @Param("minHours") int minHours, @Param("maxHours") int maxHours,
                                 @Param("minTuition") long minTuition, @Param("maxTuition") long maxTuition,
                                 Sort sort);

    // Dùng khi sửa: slug hiện tại của chính khóa đó không bị tính là trùng.
    boolean existsBySlugAndIdNot(String slug, Long id);
}
