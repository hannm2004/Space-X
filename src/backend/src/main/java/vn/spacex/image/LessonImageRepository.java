package vn.spacex.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonImageRepository extends JpaRepository<LessonImage, Long> {

    List<LessonImageInfo> findByLessonIdOrderByIdAsc(Long lessonId);

    // Dạng chiếu: lấy thông tin (kể cả lessonId để kiểm quyền) mà chưa tải dữ liệu ảnh.
    <T> Optional<T> findById(Long id, Class<T> type);
}
