import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// jsdom không cài đặt window.scrollTo (gọi vào chỉ in lỗi "not implemented"). Trang bài đưa người học về đầu trang khi
// đổi bài nên thay bằng hàm rỗng; test nào cần kiểm việc cuộn thì tự gán hàm giả của riêng nó.
// Một số test chạy ở môi trường node (đọc tệp, không dựng giao diện) nên không có window.
if (typeof window !== 'undefined') {
  window.scrollTo = vi.fn() as unknown as typeof window.scrollTo
}

// Vitest không bật biến toàn cục nên Testing Library không tự dọn; gỡ component sau mỗi test để các test không dính nhau.
afterEach(() => {
  cleanup()
})
