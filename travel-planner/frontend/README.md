# travel-planner

旅游规划智能体系统的 Vue 3 + TypeScript 前端（Vite），长期手写维护，不从模板重新生成；
与后端通过 `/api` 接口联调。

## 快速开始

```bash
npm install       # 安装依赖
npm run dev       # 开发服务器 http://localhost:5173
npm run build     # 生产构建（先做类型检查）
npm run test      # Vitest 单元测试
npm run coverage  # 覆盖率报告
npm run lint      # ESLint（带 --fix，会自动改文件）
npm run format    # Prettier（会自动改文件）
```

只想检查不改文件（CI 用的就是这两条）：`npm run lint:ci`、`npm run format:check`。

Windows 下也可以直接双击 `scripts\dev.cmd`。

## 目录结构

```
.
├── package.json          # 依赖与命令（dev / build / test / lint）
├── vite.config.ts        # 构建配置 + /api 代理到后端 8080
├── vitest.config.ts      # 单元测试配置（jsdom 环境 + V8 覆盖率）
├── eslint.config.js      # 团队统一的 ESLint 规则
├── tsconfig.json         # TypeScript 严格模式 + @ 路径别名
├── env.d.ts              # VITE_API_BASE_URL / VITE_WS_BASE_URL 类型声明
├── scripts/              # gen-api-types.mjs：由接口契约生成 src/api/schema.ts
└── src
    ├── main.ts
    ├── App.vue
    ├── api/              # axios 实例与接口封装；schema.ts 由契约生成，勿手改
    ├── components/       # 通用组件，例如图表（选了 echarts 才有）
    ├── realtime/         # WebSocket(STOMP) 连接封装（选了 stomp 才有）
    ├── router/index.ts
    ├── views/            # 页面
    └── __tests__/        # 单元测试
```

## 依赖

已引入：vue, vue-router, pinia, axios, echarts, @stomp/stompjs

构建与测试工具链（Vite / TypeScript / vue-tsc / ESLint / Prettier / Vitest）由脚手架统一带上，不需要单独安装。

## 团队规范

- 统一 UTF-8、LF、2 空格缩进（`.editorconfig`）。
- 组件只通过 `src/api` 调用后端，不直接调 axios、不在页面里拼接 URL。
- 接口地址走环境变量 `VITE_API_BASE_URL`：开发留空走 Vite 代理，生产填正式域名。
- 提交前执行 `npm run lint:ci`、`npm run format:check` 与 `npm run test`，规则集中在 `eslint.config.js` 与 `.prettierrc.json`。
- 排版（换行、缩进、引号）归 Prettier，正确性（未用变量、Vue 用法）归 ESLint；
  两条只管换行的 vue 规则已在 `eslint.config.js` 里关掉，避免和 Prettier 打架。
- 只通过 `@/` 别名引用 `src` 下的模块，避免相对路径层层回退。

## 接口类型（自动生成）

`src/api/schema.ts` 由 `npm run gen:api` 从 `../docs/openapi.json`（后端 DTO 的契约快照）生成，
**不要手改**，下次生成会被覆盖。接口封装统一从这里取类型：

```ts
import { http } from '@/api/http'
import type { ApiResponseHealthResponse } from '@/api/schema'

export async function fetchHealth(): Promise<ApiResponseHealthResponse> {
  const response = await http.get<ApiResponseHealthResponse>('/v1/health')
  return response.data
}
```

后端改了接口，先同步契约再重新生成：

```bash
npm run gen:api    # 读 ../docs/openapi.json 重新生成 src/api/schema.ts
```

CI 会重跑这条命令并比对 `schema.ts`，没跟着契约更新就失败。生成脚本只用 Node 内置模块，不引入额外依赖，
所以后端和前端可以各自独立升级依赖。

## 开发指南：新增一个页面 / 模块

| 步骤 | 放哪里 | 写什么 |
| --- | --- | --- |
| 1 | `src/api/<模块>.ts` | 接口封装：用 `src/api/http.ts` 里的实例，不在页面里直接调 axios、不手拼 URL |
| 2 | `src/views/<模块>View.vue` | 页面组件 |
| 3 | `src/router/index.ts` | 注册路由，页面用懒加载 `() => import('@/views/<模块>View.vue')` |
| 4 | `src/components/` | 可复用组件；图表直接复用现成的 `EChartsPanel.vue` |
| 5 | `src/stores/` | 跨页面共享状态时新建 Pinia store（骨架未预置，按需创建） |
| 6 | `src/__tests__/<模块>.spec.ts` | Vitest 用例，`npm run test` 直接跑 |

实时推送（生成进度、消息通知）复用 `src/realtime/stompClient.ts`，订阅地址与后端 `WebSocketConfig` 保持一致。
