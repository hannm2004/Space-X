-- Tự học trực tuyến (D40): loại khóa, module của bài, ghi danh và bù quyền cho người đang học.
-- DDL của MySQL không rollback được: file hỏng giữa chừng thì phải dọn tay, nên các bước sửa dữ liệu bên dưới
-- phải chạy được với mọi dữ liệu cũ hợp lệ.

-- Mặc định PAID: quên đặt loại thì khóa bị đóng, không lỡ tay mở miễn phí. Không suy ra từ học phí 0.
ALTER TABLE courses
    ADD COLUMN access_type ENUM ('FREE', 'PAID') NOT NULL DEFAULT 'PAID' AFTER status;

-- Khóa đang có bài là khóa chứa 31 bài Tin học cũ: chuyển sang miễn phí.
UPDATE courses c
SET c.access_type = 'FREE'
WHERE EXISTS (SELECT 1 FROM lessons l WHERE l.course_id = c.id);

-- NULL: bài chưa gắn module, hiển thị phẳng. Xóa module thì bài vẫn còn.
ALTER TABLE lessons
    ADD COLUMN module_id BIGINT NULL AFTER course_id,
    ADD CONSTRAINT fk_lessons_module FOREIGN KEY (module_id) REFERENCES course_modules (id) ON DELETE SET NULL;

CREATE TABLE enrollments (
    id         BIGINT                                       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT                                       NOT NULL,
    course_id  BIGINT                                       NOT NULL,
    status     ENUM ('ACTIVE', 'REVOKED')                   NOT NULL DEFAULT 'ACTIVE',
    source     ENUM ('FREE', 'ORDER', 'ADMIN', 'MIGRATION') NOT NULL,
    -- Chỉ có khi Admin cấp tay.
    granted_by BIGINT                                       NULL,
    created_at DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- Mỗi cặp người dùng và khóa tối đa một dòng; cột đầu là user_id nên cũng dùng cho "Khóa học của tôi".
    UNIQUE KEY uk_enrollments_user_course (user_id, course_id),
    KEY idx_enrollments_course (course_id),
    -- Không ghi ON DELETE (mặc định là chặn): không mất lịch sử ghi danh khi xóa người dùng hay khóa.
    CONSTRAINT fk_enrollments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_enrollments_granted_by FOREIGN KEY (granted_by) REFERENCES users (id),
    CONSTRAINT chk_enrollments_granted_by CHECK (source <> 'ADMIN' OR granted_by IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Ai đã có tiến độ trong một khóa thì được ghi danh khóa đó, để không ai mất quyền.
INSERT INTO enrollments (user_id, course_id, source)
SELECT DISTINCT p.user_id, l.course_id, 'MIGRATION'
FROM lesson_progress p
         JOIN lessons l ON l.id = p.lesson_id;
