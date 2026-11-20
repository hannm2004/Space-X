// Luật chung cho mọi chỗ tải ảnh lên (ảnh bài học, ảnh bìa khóa). Khớp máy chủ: ba loại ảnh, tối đa 1 MiB; máy chủ vẫn kiểm
// lại loại tệp thật từ nội dung (415) và cỡ (413), nên kiểm ở trình duyệt chỉ để báo sớm.
export const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']
export const MAX_IMAGE_BYTES = 1024 * 1024
export const IMAGE_ACCEPT = IMAGE_TYPES.join(',')
export const IMAGE_HINT = 'PNG, JPEG hoặc WebP, tối đa 1 MB'

/** Trả câu báo lỗi nếu tệp không nhận được, hoặc null nếu tệp hợp lệ. */
export function imageProblem(file: File): string | null {
  if (!IMAGE_TYPES.includes(file.type)) return 'Chỉ nhận ảnh PNG, JPEG hoặc WebP.'
  if (file.size > MAX_IMAGE_BYTES) return 'Ảnh lớn hơn 1 MB. Hãy chọn ảnh nhỏ hơn.'
  return null
}
