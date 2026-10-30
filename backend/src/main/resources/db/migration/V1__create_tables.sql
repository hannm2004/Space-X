-- Schema ban đầu của Space-X, theo docs/DATABASE_DESIGN.md.
-- Không sửa file này sau khi đã chạy trên Aiven (Flyway so checksum); muốn đổi thì viết V2.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          ENUM ('LEARNER', 'ADMIN') NOT NULL DEFAULT 'LEARNER',
    auth_provider ENUM ('LOCAL', 'GOOGLE')  NOT NULL DEFAULT 'LOCAL',
    provider_id   VARCHAR(255) NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    -- MySQL cho nhiều dòng NULL trong chỉ mục UNIQUE nên tài khoản LOCAL (provider_id NULL) không xung đột.
    UNIQUE KEY uk_users_provider (auth_provider, provider_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE courses (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    slug        VARCHAR(100) NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(500) NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_courses_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE lessons (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    course_id  BIGINT       NOT NULL,
    created_by BIGINT       NOT NULL,
    slug       VARCHAR(150) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    summary    VARCHAR(500) NULL,
    content_md MEDIUMTEXT   NOT NULL,
    status     ENUM ('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lessons_slug (slug),
    KEY idx_lessons_course_status_order (course_id, status, sort_order),
    -- RESTRICT: không xóa khóa còn bài, không xóa người dùng đã tạo bài (API trả 409).
    CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lessons_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE lesson_images (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    lesson_id    BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(50)  NOT NULL,
    size_bytes   INT          NOT NULL,
    data         MEDIUMBLOB   NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_lesson_images_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE lesson_progress (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    lesson_id      BIGINT      NOT NULL,
    status         ENUM ('IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS',
    last_viewed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    -- Mỗi học sinh có đúng một dòng cho mỗi bài; cột đầu là user_id nên cũng dùng khi lấy tiến độ của một học sinh.
    UNIQUE KEY uk_progress_user_lesson (user_id, lesson_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Dữ liệu dẫn xuất từ lessons, luôn dựng lại được.
CREATE TABLE knowledge_chunks (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    lesson_id   BIGINT       NOT NULL,
    chunk_index INT          NOT NULL,
    heading     VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_chunks_lesson_index (lesson_id, chunk_index),
    FULLTEXT KEY ft_chunks_heading_content (heading, content),
    CONSTRAINT fk_chunks_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
