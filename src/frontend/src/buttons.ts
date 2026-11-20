// Chuỗi class nút dùng chung cho mọi trang. Màu, viền, cỡ chữ nằm ở index.css (.btn, .btn-primary...); trang chỉ chọn vai trò,
// không tự ghép màu. buttonStyles.test.ts quét mã nguồn để chắc rằng không nơi nào chép lại các chuỗi này.

/** Việc chính của màn hình (Lưu, Đăng ký, Hoàn thành bài). */
export const PRIMARY = 'btn btn-primary'
/** Việc phụ, cỡ nhỏ (Sửa, Ẩn bài, Hủy, Thử lại). */
export const SECONDARY = 'btn btn-secondary btn-sm'
/** Việc không hoàn tác được, chỉ dùng ở bước xác nhận (Xác nhận xóa). */
export const DANGER = 'btn btn-danger btn-sm'
