# travel-planner

由 `scaffold-cli` 生成的 Vue 3 + TypeScript 前端骨架（Vite）。

## 快速开始

```bash
npm install     # 安装依赖
npm run dev     # 开发服务器 http://localhost:5173
npm run build   # 生产构建（先做类型检查）
npm run lint    # ESLint + Prettier 校验
```

Windows 下也可以直接双击 `scripts\dev.cmd`。

## 目录结构

```
.
├── package.json          # 依赖与命令（npm run dev / build / lint）
├── eslint.config.js      # 团队统一的 ESLint 规则
├── .prettierrc.json      # 统一格式化规则
├── tsconfig.json         # TypeScript 严格模式 + @ 路径别名
├── vite.config.ts        # 构建配置 + /api 代理到后端 8080
├── index.html
└── src
    ├── main.ts
    ├── App.vue
    ├── router/index.ts
    ├── views/HomeView.vue
    └── assets/main.css
```

## 依赖

已引入：vue, vue-router, pinia, axios

构建工具链（Vite / TypeScript / vue-tsc / ESLint / Prettier）由脚手架统一带上，不需要单独安装。

## 团队规范

- 统一 UTF-8、LF、2 空格缩进（`.editorconfig`）
- 提交前执行 `npm run lint`，规则集中在 `eslint.config.js`
- 只通过 `@/` 别名引用 `src` 下的模块，避免相对路径层层回退
