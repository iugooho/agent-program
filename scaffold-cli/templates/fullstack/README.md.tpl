# {{projectName}}

由 `scaffold-cli` 生成的全栈骨架：`{{backendDir}}/` 是 Java 后端（Spring Boot {{springBootVersion}}），
`{{frontendDir}}/` 是 Vue 3 前端（Vite），两端通过 `/api` 接口契约联调。

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
- 后端健康检查：{{healthEndpoint}}；前端封装在 `frontend/src/api/health.ts`。
- 后端 artifactId 为 `{{backendArtifactId}}`，前端包名为 `{{frontendArtifactId}}`。
- Java 版本：{{javaVersion}}；包名：{{packageName}}。

## 各自的规范

| 位置 | 编码 | 校验命令 |
| --- | --- | --- |
| `{{backendDir}}/` | UTF-8、4 空格、JDK {{javaVersion}} | `mvn -f backend/pom.xml -Pquality verify` |
| `{{frontendDir}}/` | UTF-8、2 空格、ESLint + Prettier | `npm --prefix frontend run lint` |
| 前端测试 | Vitest + jsdom | `npm --prefix frontend run test` |

生成信息（脚手架版本、选择的依赖）见根目录 `scaffold.json`。

## 开发指南：新增一个模块放哪里

分层约定：前端只通过 `/api` 调后端；后端按 `api → application → domain ← infrastructure` 单向依赖，
外部工具（大模型、机票、酒店、天气、地图）统一收敛在 `infrastructure`，领域层不出现框架注解与 SQL。

### 后端：新增一个业务模块（以「行程生成 itinerary」为例）

| 步骤 | 放哪里 | 写什么 |
| --- | --- | --- |
| 1 | `backend/src/main/java/{{packagePath}}/itinerary/domain/` | 领域模型：`Itinerary`、`DayPlan` 等实体，以及 `ItineraryRepository` 接口 |
| 2 | `backend/src/main/java/{{packagePath}}/itinerary/infrastructure/` | 仓储实现与外部适配：MyBatis-Plus Mapper、大模型/机票/酒店/天气客户端 |
| 3 | `backend/src/main/java/{{packagePath}}/itinerary/application/` | 用例编排：`ItineraryService`，管事务、调领域对象与工具、组装结果 |
| 4 | `backend/src/main/java/{{packagePath}}/itinerary/api/` | 接口层：`ItineraryController` + 请求/响应 DTO，返回统一 `ApiResponse`，入参加 `@Valid` |
| 5 | `backend/src/main/resources/db/migration/` | 迁移脚本 `V2__itinerary.sql`；**已执行过的脚本只增不改**，也不要手工改库 |
| 6 | `backend/src/test/java/{{packagePath}}/itinerary/` | 单元测试与 MockMvc 接口测试，和被测类同包 |

需要交给 Spring 管理的类加 `@Service` / `@Component` / `@Mapper` 即可，包路径在启动类扫描范围内，不用改配置。
新增配置项写进 `application.yml`，敏感项用 `${ENV_VAR:默认值}`，生产值走环境变量，不要提交进仓库。

跨模块复用：能被两个以上模块用到的实体或工具，提到 `{{packagePath}}/common/` 下，不要互相 import 对方的 `infrastructure`。

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
npm --prefix frontend run lint             # 前端：ESLint
npm --prefix frontend run test             # 前端：Vitest
```
