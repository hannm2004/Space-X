// Trang khóa gửi kèm khóa đang xem khi chuyển sang trang bài, để trang bài có nút "Quay lại <khóa>".
// API bài học không trả về khóa, nên thông tin này chỉ có khi người dùng đi từ trang khóa sang.
export type LessonLinkState = { courseSlug: string; courseTitle: string }

const SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

// Chỉ nhận đúng dạng và slug hợp lệ; sai dạng thì coi như không có, tránh dựng liên kết từ dữ liệu lạ.
export function readLessonLinkState(state: unknown): LessonLinkState | null {
  if (typeof state !== 'object' || state === null) return null
  const { courseSlug, courseTitle } = state as Record<string, unknown>
  if (typeof courseSlug !== 'string' || !SLUG.test(courseSlug)) return null
  if (typeof courseTitle !== 'string' || courseTitle.trim() === '' || courseTitle.length > 200) return null
  return { courseSlug, courseTitle }
}
