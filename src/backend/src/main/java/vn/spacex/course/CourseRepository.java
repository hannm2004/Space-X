package vn.spacex.course;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.spacex.access.AccessRules;

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

    // Các khóa người gọi được học (AccessRules), theo thứ tự hiển thị.
    @Query(value = "select c.* from courses c where" + AccessRules.COURSE_ACCESSIBLE
            + "order by c.sort_order, c.id", nativeQuery = true)
    List<Course> findAccessible(@Param("userId") long userId, @Param("admin") boolean admin);

    // 1 nếu người gọi được học khóa này, 0 nếu không được hoặc khóa không tồn tại.
    @Query(value = "select count(*) from courses c where c.id = :id and" + AccessRules.COURSE_ACCESSIBLE,
            nativeQuery = true)
    long countAccessible(@Param("id") Long id, @Param("userId") long userId, @Param("admin") boolean admin);

    // Chưa có entity Enrollment nên đếm thẳng; tính cả ghi danh đã thu hồi vì dòng đó vẫn là lịch sử.
    @Query(value = "select count(*) from enrollments e where e.course_id = :id", nativeQuery = true)
    long countEnrollments(@Param("id") Long id);

    // Dùng khi sửa: slug hiện tại của chính khóa đó không bị tính là trùng.
    boolean existsBySlugAndIdNot(String slug, Long id);
}
