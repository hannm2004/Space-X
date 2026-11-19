package vn.spacex.image;

// Cặp (bài, ảnh bìa của bài) cho truy vấn gom nhiều bài một lần.
public interface CoverRef {
    Long getLessonId();

    Long getImageId();
}
