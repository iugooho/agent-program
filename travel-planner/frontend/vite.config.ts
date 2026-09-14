import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 后端接口统一走 /api，开发时由 Vite 代理，避免跨域配置
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
