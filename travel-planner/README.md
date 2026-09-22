# travel-planner

旅游规划智能体系统的业务代码库：`backend/` 是 Java 后端（Spring Boot 3.5.16），
`frontend/` 是 Vue 3 前端（Vite），两端通过 `/api` 接口契约联调。

本目录最初由 `scaffold-cli` 生成，自 2026-09-20 起转为**长期手写维护**：
不再从模板重新生成，脚手架侧的改动也不会同步进来。业务代码直接写在这里，
禁止在本目录执行 `scaffold.ps1 init --force`（详见仓库根目录 README 第六节）。

## 快速开始

```powershell
# 后端：编译 + 单元测试
mvn -f backend/pom.xml test

# 后端：启动服务（默认 8080）
mvn -f backend/pom.xml spring-boot:run

# 前端：安装依赖并启动开发服务器（默认 http://localhost:5173）
cd frontend
npm install
npm run dev
```

Windows 下一键启动（后端在新窗口运行，前端在当前窗口）：

```powershell
.\scripts\dev.ps1
```

## 联调约定

- 前端开发服务器已把 `/api` 代理到 `http://localhost:8080`（见 `frontend/vite.config.ts`），后端监听 8080 即可。
- 后端健康检查：GET /api/v1/health；前端封装在 `frontend/src/api/health.ts`。
- 后端 artifactId 为 `travel-planner-backend`，前端包名为 `travel-planner-frontend`。
- Java 版本：21；包名：com.travelagent.travelplanner。

## 接口契约（前后端唯一可信源）

接口结构只保留一份来源：**后端的 DTO 与 Controller**，经 `docs/openapi.json` 传给前端。

```
backend DTO / Controller  --(springdoc /v3/api-docs)-->  docs/openapi.json  --(npm run gen:api)-->  frontend/src/api/schema.ts
        真源                                                 契约（提交进仓库）                          生成的 TS 类型（不手改）
```

改接口的四步，缺一步 CI 就会红：

```powershell
# 1. 改后端 DTO / Controller
# 2. 重新生成契约
mvn -f backend/pom.xml test -Dopenapi.write=true
# 3. 重新生成前端类型
npm --prefix frontend run gen:api
# 4. 把后端代码 + docs/openapi.json + frontend/src/api/schema.ts 放进同一个提交
```

两道防线：后端 `backend/src/test/java/com/travelagent/travelplanner/OpenApiContractTest.java` 保证
后端实际输出的 OpenAPI 与 `docs/openapi.json` 一致（普通 `mvn test` 就会跑到）；
CI 的前端 job 重新执行 `npm run gen:api` 并比对 `src/api/schema.ts`，保证契约与前端类型一致。
细节见 [docs/README.md](docs/README.md)。

前端引用类型一律 `import type { ApiResponseXxx } from '@/api/schema'`：不要手写接口类型，也不要手改 `schema.ts`。

## 错误码

失败响应与成功响应同构，都是 `ApiResponse`，前端按 `code` 判分支、不解析 `message`。
完整码表与用法见 [docs/error-codes.md](docs/error-codes.md)：业务代码抛
`com.travelagent.travelplanner.common.error.BusinessException`（不依赖任何框架类型），
由 `api/GlobalExceptionHandler` 统一转成响应和 HTTP 状态。

## 各自的规范

| 位置 | 编码 | 校验命令 |
| --- | --- | --- |
| `backend/` | UTF-8、4 空格、JDK 21 | `mvn -f backend/pom.xml -Pquality verify` |
| `frontend/` | UTF-8、2 空格、ESLint + Prettier | `npm --prefix frontend run lint` |
| 前端测试 | Vitest + jsdom | `npm --prefix frontend run test` |

生成信息（脚手架版本、选择的依赖）见根目录 `scaffold.json`，仅作初始结构的追溯，当前代码以本目录为准。

## 开发指南：新增一个模块放哪里

分层约定：前端只通过 `/api` 调后端；后端按 `api → application → domain ← infrastructure` 单向依赖，
外部工具（大模型、机票、酒店、天气、地图）统一收敛在 `infrastructure`，领域层不出现框架注解与 SQL。

### 后端：新增一个业务模块（以「行程生成 itinerary」为例）

| 步骤 | 放哪里 | 写什么 |
| --- | --- | --- |
| 1 | `backend/src/main/java/com/travelagent/travelplanner/itinerary/domain/` | 领域模型：`Itinerary`、`DayPlan` 等实体，以及 `ItineraryRepository` 接口 |
| 2 | `backend/src/main/java/com/travelagent/travelplanner/itinerary/infrastructure/` | 仓储实现与外部适配：MyBatis-Plus Mapper、大模型/机票/酒店/天气客户端 |
| 3 | `backend/src/main/java/com/travelagent/travelplanner/itinerary/application/` | 用例编排：`ItineraryService`，管事务、调领域对象与工具、组装结果 |
| 4 | `backend/src/main/java/com/travelagent/travelplanner/itinerary/api/` | 接口层：`ItineraryController` + 请求/响应 DTO，返回统一 `ApiResponse`，入参加 `@Valid` |
| 5 | `backend/src/main/resources/db/migration/` | 迁移脚本 `V2__itinerary.sql`；**已执行过的脚本只增不改**，也不要手工改库 |
| 6 | `backend/src/test/java/com/travelagent/travelplanner/itinerary/` | 单元测试与 MockMvc 接口测试，和被测类同包 |

需要交给 Spring 管理的类加 `@Service` / `@Component` / `@Mapper` 即可，包路径在启动类扫描范围内，不用改配置。
新增配置项写进 `application.yml`，敏感项用 `${ENV_VAR:默认值}`，生产值走环境变量，不要提交进仓库。

跨模块复用：能被两个以上模块用到的实体或工具，提到 `com/travelagent/travelplanner/common/` 下，不要互相 import 对方的 `infrastructure`。

### 前端：新增一个页面 / 模块

| 步骤 | 放哪里 | 写什么 |
| --- | --- | --- |
| 1 | `frontend/src/api/itinerary.ts` | 接口封装：用 `src/api/http.ts` 的实例，不在页面里直接调 axios、不手拼 URL |
| 2 | `frontend/src/views/ItineraryView.vue` | 页面组件 |
| 3 | `frontend/src/router/index.ts` | 注册路由，页面用懒加载 `() => import('@/views/ItineraryView.vue')` |
| 4 | `frontend/src/components/` | 可复用组件；图表直接复用现成的 `EChartsPanel.vue` |
| 5 | `frontend/src/stores/` | 需要跨页面共享状态时新建 Pinia store（骨架未预置，按需创建） |
| 6 | `frontend/src/__tests__/ItineraryView.spec.ts` | Vitest 用例，`npm run test` 直接跑 |

实时推送（行程生成进度、Agent 执行状态）复用 `frontend/src/realtime/stompClient.ts`，
后端端点是 `/ws`、订阅地址 `/topic/trips/{tripId}`（见 `WebSocketConfig`）。

### 改完自检

```powershell
mvn -f backend/pom.xml -Pquality verify    # 后端：测试 + Checkstyle
npm --prefix frontend run lint:ci          # 前端：ESLint（只检查）
npm --prefix frontend run format:check     # 前端：Prettier（只检查）
npm --prefix frontend run test             # 前端：Vitest
```

改了接口的话，前面还要加「重新生成契约 + 重新生成前端类型」两步（见上一节）。

这几条也跑在 CI 里（`.github/workflows/ci.yml`，本地路径按仓库根目录写即可），
推送和 PR 会自动执行；本地 `npm run lint` 带 `--fix` 会自动改文件，CI 用的是只检查的 `npm run lint:ci`。
