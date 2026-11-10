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
    port: 5174,
    strictPort: true,
    proxy: {
      // 后端接口统一走 /api，开发时由 Vite 代理，避免跨域配置
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure(proxy) {
          // 浏览器 POST 会带 Origin；代理请求由 Vite 发起，不应再触发后端 CORS 校验。
          proxy.on('proxyReq', (proxyRequest) => {
            proxyRequest.removeHeader('origin')
          })
        }
      }
    }
  }
})
