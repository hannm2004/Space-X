import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Khi chạy `npm run dev`, chuyển các yêu cầu /api sang backend Spring Boot.
    proxy: {
      '/api': 'http://localhost:8080',
      // Đăng nhập Google (UC03): /oauth2/... chuyển sang Google, /login/oauth2/... là nơi Google gọi lại. changeOrigin
      // đổi header Host thành localhost:8080 để Spring dựng redirect URI đúng địa chỉ đã đăng ký ở Google Cloud Console
      // (http://localhost:8080/login/oauth2/code/google); không có nó, redirect URI sẽ là cổng 5173 và Google từ chối.
      '/oauth2': { target: 'http://localhost:8080', changeOrigin: true },
      '/login/oauth2': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  test: {
    // Test component và API client cần document, window, cookie: jsdom giả lập trình duyệt.
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
})
