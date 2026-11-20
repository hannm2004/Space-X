package vn.spacex.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CourseCoverRepository extends JpaRepository<CourseCover, Long> {

    // Khóa nào có ảnh bìa và ảnh được đặt lúc nào, trong một truy vấn cho cả danh sách (không tải dữ liệu ảnh).
    @Query("select c.courseId as courseId, c.updatedAt as updatedAt from CourseCover c where c.courseId in :ids")
    List<CourseCoverVersion> findVersions(@Param("ids") Collection<Long> ids);
}
