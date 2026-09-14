# 项目脚手架生成器（scaffold-cli）

本仓库是一套**统一规范的项目脚手架生成器**：一条命令生成可编译、可测试、规范统一的 Java / Vue / 全栈项目骨架。

```powershell
cd scaffold-cli
.\scaffold.ps1 list                                        # 查看可选项
.\scaffold.ps1 init my-service --type backend              # 生成 Java 后端
.\scaffold.ps1 init my-web --type frontend                 # 生成 Vue 前端
.\scaffold.ps1 init my-platform --type fullstack           # 生成全栈（backend + frontend）
```

生成器只产出骨架和规范配置，不含任何业务逻辑。

## 一、生成器能做什么

| 要求 | 实现 |
| --- | --- |
| 生成目录结构 | `src/main/java`、`src/main/resources`、`src/test/java`、`scripts/`，前端整套 Vue 目录 |
| 生成配置文件 | 后端 `pom.xml`；前端 `package.json`、`tsconfig.json`、`vite.config.ts`、`eslint.config.js`、`.prettierrc.json` |
| 自动引入基础依赖 | Java：spring-core / spring-context / junit / logback / slf4j / jackson / lombok；前端：vue / vue-router / pinia / axios（vite、typescript、eslint、prettier 自动附带） |
| 统一项目规范 | UTF-8 编码、`maven.compiler.release`、`.editorconfig`、`.gitignore`、`-Xlint:all`、Checkstyle 3.6.0、ESLint + Prettier |
| 内置脚本命令 | 后端 `mvn compile` / `mvn test` / `mvn exec:java` / `mvn -Pquality verify`；前端 `npm run dev` / `build` / `lint` / `format` |

三种项目类型：`backend`（11 个文件）、`frontend`（19 个文件）、`fullstack`（35 个文件，挂成 `backend/` + `frontend/`）。

## 二、生成器用法

```powershell
cd scaffold-cli

# 常用写法
.\scaffold.ps1 init my-app --type backend --group com.acme
.\scaffold.ps1 init my-shop --type fullstack --deps spring-core,junit,logback,jackson --output ..
.\scaffold.ps1 init demo --type frontend --frontend-deps vue,vue-router,pinia --no-interactive

# 只预览要生成哪些文件，不写盘
java -jar .\target\scaffold-cli-1.0.0.jar init demo --type fullstack --dry-run --no-interactive
```

`scaffold.ps1` 首次运行会自动执行 `mvn package` 打包；Windows 下也可以用 `scaffold.cmd`。参数速查：

```
-t --type  -g --group  -a --artifact  -p --package  -v --version  -j --java
-d --deps  -f --frontend-deps  -o --output  --description  --templates
--force  --dry-run  -i --interactive  --no-interactive
```

模板是数据不是代码：`scaffold-cli/templates/` 下 35 个模板用 `{{key}}` 占位符渲染（目录名也会渲染，`.tpl` 后缀自动去掉），用 `--templates` 或 `SCAFFOLD_TEMPLATES` 可整体替换，团队换风格不用改 Java 代码。

## 三、配套骨架：travel-planner

`travel-planner/` 是用该生成器产出的全栈骨架（旅游规划智能体系统的落地起点），已经是可编译、可测试的状态：

```powershell
# 后端（Java 21 + Maven）
cd travel-planner\backend
mvn compile            # 编译
mvn test               # 单元测试
mvn exec:java          # 运行 App
mvn "-Pquality" verify # Checkstyle 规范检查

# 前端（Vue 3 + TypeScript + Vite，dev 服务器 http://localhost:5173，/api 已代理到 8080）
cd travel-planner\frontend
npm install
npm run dev
npm run build          # 生产构建
npm run lint           # ESLint

# 一键起前后端
cd travel-planner
.\scripts\dev.ps1
```

目录结构：

```
scaffold-cli/                     生成器（Java 源码 + 35 个模板）
├── src/main/java/com/example/scaffold/
└── templates/                    backend / frontend / fullstack 三套模板

travel-planner/                   生成出来的全栈骨架
├── backend/                      Maven 工程（App.java + 冒烟测试 + Checkstyle + logback）
├── frontend/                     Vue 3 + TS + Vite（router / ESLint / Prettier）
├── scripts/dev.ps1               一键起后端与前端
└── scaffold.json                 生成元信息（生成器版本、时间、类型、依赖）
```

## 四、环境要求与已验证状态

需要 JDK 21+（本机实测 Java 25）、Maven 3.9、Node 22 / npm 10。

- 生成器自身单元测试 17 个全部通过
- `travel-planner/backend`：`mvn clean test` → Tests run: 1, Failures: 0；`-Pquality verify` → 0 Checkstyle violations
- `travel-planner/frontend`：`npm install`（197 个包）→ `npm run build`、`npm run lint` 均通过

## 五、变更说明

仓库里原来的手搭多模块旅游工程（travel-* 十个模块、根 pom.xml、start-local.ps1/.cmd、docker-compose.yml、.env.example、.run/ 等）已按需求整体移除，旧文件备份在 `%TEMP%\travel-scaffold-backup-20260914-202204`，需要时可以从该目录整体还原。
