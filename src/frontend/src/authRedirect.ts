// Trang bài gửi kèm `from` (trang đang đọc) khi mời đăng nhập, để đăng nhập xong quay lại đúng chỗ.
// Chỉ nhận đường dẫn nội bộ: chặn địa chỉ sang trang khác (open redirect) và chính trang đăng nhập/đăng ký (quay vòng).
const AUTH_PAGES = ['/dang-nhap', '/dang-ky']
const MAX_LENGTH = 200

function hasUnsafeCharacter(text: string): boolean {
  for (const char of text) {
    const code = char.codePointAt(0) ?? 0
    if (code <= 0x20 || code === 0x7f || char === '\\') return true
  }
  return false
}

export function readRedirectPath(state: unknown): string | null {
  if (typeof state !== 'object' || state === null) return null
  const from = (state as Record<string, unknown>).from
  if (typeof from !== 'string' || from.length > MAX_LENGTH) return null
  // Phải bắt đầu bằng đúng một dấu "/" ("//host" là địa chỉ sang trang khác).
  if (!from.startsWith('/') || from.startsWith('//')) return null
  if (hasUnsafeCharacter(from)) return null
  const path = from.split(/[?#]/)[0]
  if (AUTH_PAGES.includes(path)) return null
  return from
}
