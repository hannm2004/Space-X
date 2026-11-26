-- Danh mục khóa của Trung tâm Tin học SPACE-X (D38, D39; docs/DATABASE_DESIGN.md mục 8).
-- DDL của MySQL không rollback được: file hỏng giữa chừng thì phải dọn tay, nên các bước sửa dữ liệu bên dưới
-- phải chạy được với mọi dữ liệu cũ hợp lệ.

CREATE TABLE categories (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    slug       VARCHAR(100) NOT NULL,
    name       VARCHAR(150) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm của 31 bài Tin học cũ, cũng là nhóm mặc định của khóa tạo mới cho tới khi Admin chọn được nhóm.
INSERT INTO categories (slug, name) VALUES ('khoa-cho-thieu-nien', 'Khóa cho thiếu niên');

-- category_id và code tạm cho NULL để còn điền dữ liệu cho các khóa đã có.
ALTER TABLE courses
    ADD COLUMN category_id   BIGINT                                  NULL AFTER id,
    ADD COLUMN code          VARCHAR(20)                             NULL AFTER category_id,
    ADD COLUMN level         ENUM ('BASIC', 'ADVANCED', 'EXPERT')    NOT NULL DEFAULT 'BASIC',
    ADD COLUMN course_type   ENUM ('SHORT', 'LONG')                  NOT NULL DEFAULT 'SHORT',
    ADD COLUMN total_hours   SMALLINT UNSIGNED                       NOT NULL DEFAULT 0,
    ADD COLUMN sessions      SMALLINT UNSIGNED                       NOT NULL DEFAULT 0,
    -- Đồng Việt Nam, số nguyên. 0 nghĩa là chưa có học phí nên chưa mở đăng ký.
    ADD COLUMN tuition_vnd   BIGINT                                  NOT NULL DEFAULT 0,
    -- description (500 ký tự) là câu ngắn trên thẻ khóa; overview_md là mục "Mô tả" đầy đủ ở trang khóa.
    ADD COLUMN overview_md   TEXT                                    NULL,
    ADD COLUMN audience_md   TEXT                                    NULL,
    ADD COLUMN outcomes_md   TEXT                                    NULL,
    ADD COLUMN assessment_md TEXT                                    NULL,
    ADD COLUMN status        ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT';

-- Khóa đã có đang hiển thị công khai nên giữ PUBLISHED.
UPDATE courses
SET category_id = (SELECT id FROM categories WHERE slug = 'khoa-cho-thieu-nien'),
    status      = 'PUBLISHED';

-- Mã khóa = chữ hoa của slug bỏ gạch ngang (tin-hoc-10 thành TINHOC10). Slug quá 20 ký tự, hoặc hai slug ra cùng
-- một mã, thì dùng KH-<id>: mã lấy từ slug không bao giờ có gạch ngang nên hai kiểu không đụng nhau.
UPDATE courses c
    LEFT JOIN (SELECT REPLACE(slug, '-', '') AS compact
               FROM courses
               GROUP BY REPLACE(slug, '-', '')
               HAVING COUNT(*) > 1) duplicated ON duplicated.compact = REPLACE(c.slug, '-', '')
SET c.code = IF(CHAR_LENGTH(REPLACE(c.slug, '-', '')) <= 20 AND duplicated.compact IS NULL,
                UPPER(REPLACE(c.slug, '-', '')),
                CONCAT('KH-', c.id));

ALTER TABLE courses
    MODIFY COLUMN category_id BIGINT      NOT NULL,
    MODIFY COLUMN code        VARCHAR(20) NOT NULL,
    ADD UNIQUE KEY uk_courses_code (code),
    ADD KEY idx_courses_category_status_order (category_id, status, sort_order),
    -- RESTRICT: không xóa nhóm còn khóa.
    ADD CONSTRAINT fk_courses_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_courses_tuition CHECK (tuition_vnd >= 0);

ALTER TABLE courses
    ADD FULLTEXT KEY ft_courses_title_description (title, description);

CREATE TABLE course_modules (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    course_id  BIGINT       NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    title      VARCHAR(200) NOT NULL,
    content_md TEXT         NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_course_modules_course_order (course_id, sort_order),
    CONSTRAINT fk_course_modules_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE course_faqs (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    course_id  BIGINT       NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    question   VARCHAR(300) NOT NULL,
    answer     TEXT         NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_course_faqs_course_order (course_id, sort_order),
    CONSTRAINT fk_course_faqs_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Ảnh dùng chung cho banner, giáo viên và bài viết; lưu trong database như lesson_images (Render Free không giữ file).
CREATE TABLE site_images (
    id           BIGINT                             NOT NULL AUTO_INCREMENT,
    kind         ENUM ('BANNER', 'TEACHER', 'POST') NOT NULL,
    file_name    VARCHAR(255)                       NOT NULL,
    content_type VARCHAR(50)                        NOT NULL,
    size_bytes   INT                                NOT NULL,
    data         MEDIUMBLOB                         NOT NULL,
    created_at   DATETIME(6)                        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)                        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE banners (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    image_id   BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    link_path  VARCHAR(255) NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_banners_active_order (active, sort_order),
    -- RESTRICT: banner không có ảnh thì vô nghĩa, phải gỡ banner trước.
    CONSTRAINT fk_banners_image FOREIGN KEY (image_id) REFERENCES site_images (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
