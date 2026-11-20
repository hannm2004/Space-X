// Chuỗi class dùng chung cho các trang quản trị nội dung (khóa học, bài học).
// Nút dùng chung ở mọi trang; giữ re-export để các trang quản trị không phải đổi chỗ import.
export { DANGER, SECONDARY } from './buttons'
export const CARD = 'rounded-xl border border-line bg-surface p-4 shadow-card'
export const SUCCESS_BOX =
  'mt-4 rounded-lg border border-success/40 bg-success-soft px-4 py-3 text-success'
export const NO_PERMISSION = 'Bạn không có quyền truy cập trang này.'
