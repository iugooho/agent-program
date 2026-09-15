import { defineConfig, mergeConfig } from 'vitest/config'

import viteConfig from './vite.config'

// 复用 vite.config.ts 的别名与插件，只补充测试相关配置
export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      include: ['src/**/*.spec.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html']
      }
    }
  })
)
