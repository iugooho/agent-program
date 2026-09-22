# AGENTS.md

在这个仓库里干活（人或者 AI）之前先读这一页，再读具体目录的 README。
详细的背景在根 `README.md`，这里只放"必须遵守什么"。

## 这是什么仓库

- `scaffold-cli/`：项目脚手架生成器（Java + Maven），用模板生成 Java / Vue / 全栈骨架。脚手架本体。
- `travel-planner/`：旅游规划智能体系统的业务代码库，`backend/`（Spring Boot 3 + JDK 21）+ `frontend/`（Vue 3 + TS）。长期手写维护。

两者独立演进：改脚手架不会同步进 `travel-planner/`，反之亦然。

## 铁律

1. **不要对 `travel-planner/` 执行 `scaffold.ps1 init --force`。** 那是业务代码库，`--force` 只覆盖与模板同路径的文件、不删除其它文件，结果是骨架层（README、`pom.xml`、`application.yml`、`App.java`、`vite.config.ts` 等）被回退到模板初始内容，而手写的业务模块原样留着，两边对不上。要试模板效果用 `--dry-run`，或在 `scaffold-cli/` 里另建目录生成。
2. **改 `scaffold-cli/templates/` 之后必须 `mvn -B clean package`。** `clean` 会清掉 `target/templates`，否则生成器可能还在用旧模板。
3. **不要手改生成物**：`travel-planner/docs/openapi.json`、`travel-planner/frontend/src/api/schema.ts`。它们由脚本生成，改了也会被覆盖，CI 还会拦。

## 改完必须自检

| 改了哪里 | 跑什么 |
| --- | --- |
| `travel-planner/backend/` | `mvn -B -f travel-planner/backend/pom.xml -Pquality verify` |
| `travel-planner/frontend/` | `npm --prefix travel-planner/frontend run lint:ci` + `run format:check` + `run test` + `run build` |
| `scaffold-cli/` | `mvn -B -f scaffold-cli/pom.xml clean verify -Pquality` |
| 接口（DTO / Controller） | 见下面「接口契约」 |

同一套命令跑在 CI 里（`.github/workflows/ci.yml`），PR 必须全绿才能合。

## 接口契约

唯一可信源是后端的 DTO 与 Controller，前端类型由契约生成，不要手写：

```
backend DTO / Controller  ->  travel-planner/docs/openapi.json  ->  frontend/src/api/schema.ts
```

改接口之后：

```powershell
mvn -B -f travel-planner/backend/pom.xml test -Dopenapi.write=true   # 1. 由后端 DTO 刷新契约
npm --prefix travel-planner/frontend run gen:api                     # 2. 由契约刷新前端类型
mvn -B -f travel-planner/backend/pom.xml -Pquality verify            # 3. 自检
```

后端代码、`docs/openapi.json`、`src/api/schema.ts` 三份改动放**同一个提交**。
后端 `OpenApiContractTest` 挡「改了接口忘更新契约」，CI 前端 job 挡「更新了契约忘生成类型」。
前端引用类型一律 `import type { ApiResponseXxx } from '@/api/schema'`。

## 规范都在哪

- 编辑格式：根 `.editorconfig`（UTF-8 / LF / 末尾留空行 / 去尾随空格；Java 4 空格、其余 2 空格）
- 后端代码风格：`travel-planner/backend/config/checkstyle.xml`，由 `mvn -Pquality verify` 强制执行，**告警即失败**
- 前端代码风格：`travel-planner/frontend/eslint.config.js` + `.prettierrc.json`，CI 跑 `lint:ci`（只检查）与 `format:check`
- 后端分层：`api -> application -> domain <- infrastructure`；`domain` 不出现框架注解与 SQL，外部工具（大模型、机票、酒店、天气、地图）收在 `infrastructure`；跨模块共用代码放 `common/`
- 前端分层：只在 `src/api/` 里调后端，不直接调 axios、不手拼 URL，一律用 `@/` 别名
- 数据库：结构变更只走 `travel-planner/backend/src/main/resources/db/migration/V<编号>__<描述>.sql`，**已执行过的脚本只增不改**，不手工改库
- 错误码：见 `travel-planner/docs/error-codes.md`；抛 `BusinessException` 或参数校验失败，由全局异常处理器统一转成 `ApiResponse`

## 提交与评审

- 一次提交只做一件事，提交信息用祈使句（例如「补接口契约同步检查」）。
- 接口改动必须和契约、前端类型在同一个提交里。
- `.github/CODEOWNERS` 指定了各目录的 reviewer；改 `docs/openapi.json` 或 `codeowners` 涉及的目录会自动请求对应的人。
- PR 模板在 `.github/pull_request_template.md`，按上面的自检表把跑过的命令勾上。
