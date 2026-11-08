package vn.spacex.image;

// Thông tin ảnh không kèm dữ liệu nhị phân. Spring Data chỉ SELECT đúng các cột này.
public interface LessonImageInfo {

    Long getId();

    Long getLessonId();

    String getFileName();

    String getContentType();

    int getSizeBytes();
}
