import { ApiError } from './client'

// Đổi lỗi từ máy chủ thành các câu hiện cho người dùng. Chỉ dùng câu do backend viết cho người dùng
// (`detail`, danh sách lỗi theo trường); 403 và 5xx dùng câu của frontend vì nội dung thô không hữu ích.
export function errorMessages(error: unknown, fallback: string): string[] {
  if (!(error instanceof ApiError)) return [fallback]
  if (error.status === 0) return ['Không kết nối được tới máy chủ. Kiểm tra mạng rồi thử lại.']
  // 403 ở các form này gần như luôn là thiếu hoặc sai CSRF token (docs/API.md).
  if (error.status === 403) {
    return ['Phiên làm việc đã hết hạn hoặc không hợp lệ. Hãy tải lại trang rồi thử lại.']
  }
  if (error.status >= 500) return ['Máy chủ đang gặp lỗi. Hãy thử lại sau.']
  if (error.fieldErrors.length > 0) return error.fieldErrors.map((entry) => entry.message)
  return [error.detail ?? fallback]
}
