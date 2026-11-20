-- Ảnh bìa khóa học (D35): mỗi khóa tối đa một ảnh. Khóa chính chính là course_id nên "một ảnh mỗi khóa" do database giữ,
-- không cần cờ như ảnh bìa bài học (V4). Ảnh nằm riêng một bảng để danh sách khóa không phải tải dữ liệu ảnh.
-- Xóa khóa thì ảnh bìa tự mất (CASCADE).
CREATE TABLE course_covers (
    course_id    BIGINT      NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes   INT         NOT NULL,
    data         MEDIUMBLOB  NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (course_id),
    CONSTRAINT fk_course_covers_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
