import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Vitest không bật biến toàn cục nên Testing Library không tự dọn; gỡ component sau mỗi test để các test không dính nhau.
afterEach(() => {
  cleanup()
})
