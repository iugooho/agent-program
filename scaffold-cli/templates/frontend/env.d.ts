/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端接口地址：开发环境留空走 Vite 代理 /api，生产填正式域名 */
  readonly VITE_API_BASE_URL?: string
  /** WebSocket 地址，例如 ws://localhost:8080/ws */
  readonly VITE_WS_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
