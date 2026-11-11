-- Tìm kiếm bài học (D29): một chỉ mục FULLTEXT gộp tiêu đề, tóm tắt và nội dung. Độ liên quan tính chung cho cả ba cột.
-- InnoDB bỏ qua từ ngắn hơn innodb_ft_min_token_size (mặc định 3 ký tự) và từ trong danh sách stopword tiếng Anh.
ALTER TABLE lessons
    ADD FULLTEXT KEY ft_lessons_title_summary_content (title, summary, content_md);
