import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Khi chạy `npm run dev`, chuyển các yêu cầu /api sang backend Spring Boot.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
