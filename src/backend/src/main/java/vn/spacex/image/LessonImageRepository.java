package vn.spacex.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonImageRepository extends JpaRepository<LessonImage, Long> {

    List<LessonImageInfo> findByLessonIdOrderByIdAsc(Long lessonId);

    // Dạng chiếu: lấy thông tin (kể cả lessonId để kiểm quyền) mà chưa tải dữ liệu ảnh.
    <T> Optional<T> findById(Long id, Class<T> type);

    // Bỏ cờ ảnh bìa của mọi ảnh trong bài (trước khi đặt ảnh bìa mới).
    @Modifying(clearAutomatically = true)
    @Query("update LessonImage i set i.cover = false where i.lessonId = :lessonId and i.cover = true")
    int clearCover(@Param("lessonId") Long lessonId);

    @Modifying(clearAutomatically = true)
    @Query("update LessonImage i set i.cover = :cover where i.id = :id")
    int updateCover(@Param("id") Long id, @Param("cover") boolean cover);

    // Ảnh bìa của nhiều bài trong một truy vấn (không tải dữ liệu ảnh).
    @Query("select i.lessonId as lessonId, i.id as imageId from LessonImage i "
            + "where i.cover = true and i.lessonId in :lessonIds")
    List<CoverRef> findCovers(@Param("lessonIds") Collection<Long> lessonIds);
}
