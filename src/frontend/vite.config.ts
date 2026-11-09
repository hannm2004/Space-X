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
    },
  },
  test: {
    // Test component và API client cần document, window, cookie: jsdom giả lập trình duyệt.
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
})
