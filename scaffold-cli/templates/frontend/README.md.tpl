# {{projectName}}

由 `scaffold-cli` 生成的 Vue 3 + TypeScript 前端骨架（Vite），与后端通过 `/api` 接口联调。

## 快速开始

```bash
npm install       # 安装依赖
npm run dev       # 开发服务器 http://localhost:5173
npm run build     # 生产构建（先做类型检查）
npm run test      # Vitest 单元测试
npm run coverage  # 覆盖率报告
npm run lint      # ESLint + Prettier 校验
```

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
└── src
    ├── main.ts
    ├── App.vue
    ├── api/              # axios 实例与接口封装（选了 axios 才有）
    ├── components/       # 通用组件，例如图表（选了 echarts 才有）
    ├── realtime/         # WebSocket(STOMP) 连接封装（选了 stomp 才有）
    ├── router/index.ts
    ├── views/            # 页面
    └── __tests__/        # 单元测试
```

## 依赖

已引入：{{frontendDependencyIds}}

构建与测试工具链（Vite / TypeScript / vue-tsc / ESLint / Prettier / Vitest）由脚手架统一带上，不需要单独安装。

## 团队规范

- 统一 UTF-8、LF、2 空格缩进（`.editorconfig`）。
- 组件只通过 `src/api` 调用后端，不直接调 axios、不在页面里拼接 URL。
- 接口地址走环境变量 `VITE_API_BASE_URL`：开发留空走 Vite 代理，生产填正式域名。
- 提交前执行 `npm run lint` 与 `npm run test`，规则集中在 `eslint.config.js`。
- 只通过 `@/` 别名引用 `src` 下的模块，避免相对路径层层回退。

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
