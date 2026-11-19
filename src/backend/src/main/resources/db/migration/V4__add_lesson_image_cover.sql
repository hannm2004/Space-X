-- Ảnh bìa bài học (D33): mỗi bài tối đa một ảnh bìa, đánh dấu ngay trên ảnh để tránh khóa ngoại vòng giữa
-- lessons và lesson_images. Quy tắc "một ảnh bìa mỗi bài" do ImageService giữ (bỏ cờ các ảnh khác rồi đặt cờ ảnh mới
-- trong cùng một transaction). Xóa ảnh bìa thì bài tự hết ảnh bìa.
ALTER TABLE lesson_images
    ADD COLUMN is_cover TINYINT(1) NOT NULL DEFAULT 0 AFTER size_bytes;
