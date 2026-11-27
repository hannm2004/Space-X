import type { CourseLevel } from './catalogApi'

// Chữ hiển thị dùng chung cho trang công khai và trang quản trị.
export const LEVEL_LABELS: Record<CourseLevel, string> = {
  BASIC: 'Cơ bản',
  ADVANCED: 'Nâng cao',
  EXPERT: 'Chuyên sâu',
}

export function formatVnd(amount: number): string {
  return `${amount.toLocaleString('vi-VN')} đ`
}
