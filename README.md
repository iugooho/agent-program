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
| 自动引入基础依赖 | Java 默认 spring-boot-web / spring-boot-test / logback，另有 mybatis-plus、flyway、postgresql、h2、redis、rabbitmq、websocket、spring-boot-security、validation、actuator、springdoc、druid、log4j2 等共 24 个可选项；前端默认 vue / vue-router / pinia / axios，另有 echarts、stomp（vite、typescript、eslint、prettier、vitest 自动附带） |
| 按依赖挂载功能模板 | 选 Spring Boot 就生成启动类 + REST 示例 + 统一响应；选 MyBatis-Plus / Flyway / PostgreSQL 就带持久层配置、迁移脚本与数据源；选 WebSocket 就带 STOMP 配置；前端选 ECharts 就带图表组件与数据大屏路由 |
| 统一项目规范 | UTF-8 编码、`maven.compiler.release`、`.editorconfig`、`.gitignore`、`-Xlint:all`、Checkstyle 3.6.0、ESLint + Prettier、Vitest |
| 内置脚本命令 | 后端 `mvn test` / `mvn spring-boot:run` / `mvn -Pquality verify`；前端 `npm run dev` / `build` / `lint` / `test` / `format` |
| 自带能跑的测试 | 后端生成 AppTest / ApiResponseTest / HealthControllerTest（MockMvc，不绑定端口）；前端生成 Vitest + jsdom 用例，生成完 `mvn test`、`npm run test` 直接是绿的 |

三种项目类型（按默认依赖实测）：`backend` 17 个文件、`frontend` 23 个文件、`fullstack` 45 个文件（挂成 `backend/` + `frontend/`）；勾选的功能越多，生成的文件越多。

## 二、生成器用法

```powershell
cd scaffold-cli

# 常用写法
.\scaffold.ps1 init my-app --type backend --group com.acme
.\scaffold.ps1 init my-api --type backend --deps spring-boot-web,spring-boot-security,mybatis-plus,flyway,postgresql,actuator,springdoc
.\scaffold.ps1 init my-shop --type fullstack --deps spring-boot-web,mybatis-plus,h2 --frontend-deps vue,vue-router,pinia,axios,echarts,stomp --output ..
.\scaffold.ps1 init demo --type frontend --frontend-deps vue,vue-router,pinia,axios --no-interactive

# 只预览要生成哪些文件，不写盘
java -jar .\target\scaffold-cli-1.0.0.jar init demo --type fullstack --dry-run --no-interactive
```

`scaffold.ps1` 首次运行会自动执行 `mvn package` 打包；Windows 下也可以用 `scaffold.cmd`。参数速查：

```
-t --type  -g --group  -a --artifact  -p --package  -v --version  -j --java
-d --deps  -f --frontend-deps  -o --output  --description  --templates
--force  --dry-run  -i --interactive  --no-interactive
```

模板是数据不是代码：`scaffold-cli/templates/` 下 15 个模板目录、55 个模板文件，用 `{{key}}` 占位符渲染（目录名也会渲染，`.tpl` 后缀自动去掉）。最终项目 = 基础模板（common / backend / frontend）+ 按依赖条件挂载的功能模板（backend-boot / backend-mybatis / backend-flyway / backend-security / backend-websocket / backend-log4j2 / backend-datasource-* / frontend-api / frontend-echarts / frontend-stomp）；用 `--templates` 或 `SCAFFOLD_TEMPLATES` 可整体替换目录，团队换风格不用改 Java 代码。

## 三、配套骨架：travel-planner

`travel-planner/` 是用该生成器产出的全栈骨架（旅游规划智能体系统的落地起点），已经是可编译、可测试的状态：

```powershell
# 后端（Java 21 + Spring Boot 3.5.16 + Maven）
cd travel-planner\backend
mvn test               # 单元测试 + MockMvc 接口测试
mvn spring-boot:run    # 启动服务（默认 8080）
mvn -Pquality verify   # Checkstyle 规范检查 + 打包

# 前端（Vue 3 + TypeScript + Vite，dev 服务器 http://localhost:5173，/api 已代理到 8080）
cd travel-planner\frontend
npm install
npm run dev
npm run build          # 生产构建
npm run lint           # ESLint
npm run test           # Vitest 单元测试

# 一键起前后端
cd travel-planner
.\scripts\dev.ps1
```

目录结构：

```
scaffold-cli/                     生成器（Java 源码 + 15 个模板目录 / 55 个模板文件）
├── src/main/java/com/example/scaffold/
└── templates/                    common / backend / frontend / fullstack + 11 个功能模板

travel-planner/                   生成出来的全栈骨架
├── backend/                      Spring Boot 3.5.16 + MyBatis-Plus + Flyway + Security + WebSocket(STOMP)
├── frontend/                     Vue 3 + TS + Vite + Pinia + ECharts + STOMP 客户端
├── scripts/dev.ps1               一键起后端与前端
└── scaffold.json                 生成元信息（生成器版本、时间、类型、依赖）
```

## 四、环境要求与已验证状态

需要 JDK 21+（本机实测 Java 25）、Maven 3.9、Node 22 / npm 10。

- 生成器自身单元测试 24 个全部通过：`mvn -B clean test` → Tests run: 24, Failures: 0, Errors: 0
- `travel-planner/backend`：`mvn -B clean test` → Tests run: 4, Failures: 0；`mvn -B -Pquality verify` → 0 Checkstyle violations、BUILD SUCCESS，Spring Boot 可执行 jar 44737760 字节
- `travel-planner/frontend`：`npm install`（345 个包）→ `npm run lint`、`npm run test`（1 test passed）、`npm run build` 全部通过；ECharts 改成按需引入后，数据大屏 chunk 从 1037.79 kB 降到 466.50 kB（gzip 157.39 kB）

本机 JDK 25 有 NIO selector 限制，内嵌 Tomcat 无法建立回环连接（`standardService.connector.startFailed`），所以后端端到端验证走 MockMvc 集成测试（不绑定端口）；换 JDK 17 / 21 后 `mvn spring-boot:run` 可正常启动，这不是脚手架自身的问题。

## 五、需求文档

`旅游规划智能体系统_Java技术方案.docx` 是本项目的技术方案（16 章，含技术栈选型与替换说明、前后端分离设计、部署与 CI/CD），由同目录的 `generate_travel_doc.py` 生成，改完脚本重新执行即可刷新文档：

```powershell
python generate_travel_doc.py
```

## 六、后续开发指南

仓库里有两块东西，定位已经分开，**各改各的，互不覆盖**：

| 目录 | 定位 | 改动方式 |
| --- | --- | --- |
| `scaffold-cli/` | 脚手架本体：Java 代码 + `templates/` 模板 | 加依赖改 catalog，加功能改模板 + `ScaffoldEngine#mounts`；产出的是**新项目**，不回写 `travel-planner/` |
| `travel-planner/` | 正式业务代码库（旅游规划智能体系统的落地代码） | 直接改代码、按模块加业务；**不再从模板重新生成**，脚手架侧的改动不会同步进来 |

`travel-planner/` 最初由 `scaffold-cli` 生成，自 2026-09-20 起转为长期手写维护，
按普通业务项目对待。脚手架后续的迭代只服务于新生成的项目，两边从此各自演进。

下面 6.1、6.2 只涉及 `scaffold-cli/`，不会影响 `travel-planner/`；6.3 是业务侧的入口。

### 6.1 新增一个可选依赖（脚手架侧）

1. 在 `scaffold-cli/src/main/java/com/example/scaffold/core/DependencyCatalog.java` 的 static 块 `register(new Dependency(...))` 加一行；前端依赖加在 `FrontendDependencyCatalog.java`。
2. 依赖之间有约束（日志实现互斥、需要配套模块、需要默认驱动）就在 `normalize()` 里补规则，并用 `warnings` 回写提示。
3. 这个依赖还需要附带代码时，新建 `scaffold-cli/templates/<模板名>/`，在 `ScaffoldEngine#mounts` 里按依赖条件挂载，必要时声明 `excludedPaths`。
4. 在 `DependencyCatalogTest` / `ScaffoldEngineTest` 里补断言，然后 `mvn -B clean test`。

### 6.2 新增一种项目类型（脚手架侧）

`ProjectType` 加枚举值 → 新建 `templates/<类型>/` → 在 `ScaffoldEngine#mounts` 里登记目标子目录与挂载条件 → 补测试。

### 6.3 新增业务模块（业务侧）

业务代码直接写在 `travel-planner/` 里，目录约定见该目录下的 README：后端模块分包与四层结构、Controller/DTO 放哪、迁移脚本怎么编号、前端页面与接口封装放哪，见
[travel-planner/README.md](travel-planner/README.md) 的「开发指南：新增一个模块放哪里」，以及
[travel-planner/backend/README.md](travel-planner/backend/README.md)、[travel-planner/frontend/README.md](travel-planner/frontend/README.md)。

### 6.4 两条铁律

1. **禁止对 `travel-planner/` 执行 `--force` 重新生成。**它是业务代码库。`--force` 只覆盖与模板同路径的文件、不删除其它文件，所以损失形态是骨架层（README、`pom.xml`、`application.yml`、`App.java`、`vite.config.ts` 等）被整体回退到模板初始内容，而手写的业务模块原样留着——两边对不上，编译和运行都会出问题。要试模板效果就在 `scaffold-cli/` 内用 `--dry-run` 验证，或另建目录生成新项目。
2. 改完 `scaffold-cli/templates/` 必须 `mvn -B clean package`：`clean` 会清掉 `target/templates`，否则生成器可能还在用旧模板。

### 6.5 提交前自检

改业务代码（`travel-planner/`）时必跑：

```powershell
mvn -B -f travel-planner\backend\pom.xml -Pquality verify    # 后端：测试 + Checkstyle
npm --prefix travel-planner\frontend run lint:ci             # 前端：ESLint（只检查）
npm --prefix travel-planner\frontend run format:check        # 前端：Prettier（只检查）
npm --prefix travel-planner\frontend run test                # 前端：Vitest
npm --prefix travel-planner\frontend run build               # 前端：类型检查 + 生产构建
```

只是改了脚手架本体（`scaffold-cli/`）时才需要：

```powershell
cd scaffold-cli; mvn -B clean verify -Pquality               # 生成器自测：24 个用例 + Checkstyle
```

上面这些命令已经配进 `.github/workflows/ci.yml`，推送到 `main` 和提 PR 时自动执行，
三个 job 并行跑（后端 / 前端 / 脚手架）。本地自检是为了快速反馈，CI 才是合并前的硬约束。

注：CI 里前端跑的是 `lint:ci`（`eslint .`）与 `format:check`（`prettier --check`），两者都只检查；
本地的 `npm run lint`（`--fix`）与 `npm run format` 会自动改文件——故意不同，本地改、CI 验。

要让它真正挡住合并，还要在 GitHub 仓库 Settings → Branches 给 `main` 加保护规则，
勾 Require status checks 并选中上面三个 job；否则 CI 红了也照样能合。

### 6.6 改接口：契约先行

接口契约 `travel-planner/docs/openapi.json` 由后端 DTO 生成、前端从它生成 TypeScript 类型，
是前后端唯一的可信源（详见 [travel-planner/docs/README.md](travel-planner/docs/README.md)）：

```powershell
mvn -B -f travel-planner\backend\pom.xml test -Dopenapi.write=true   # 1. 由后端 DTO 刷新契约
npm --prefix travel-planner\frontend run gen:api                     # 2. 由契约刷新前端类型
```

后端代码、`docs/openapi.json`、`src/api/schema.ts` 三份改动必须进同一个提交：
后端的 `OpenApiContractTest` 挡住「改了接口忘更新契约」，CI 前端 job 挡住「更新了契约忘生成类型」。

## 七、变更说明

仓库里原来的手搭多模块旅游工程（travel-* 十个模块、根 pom.xml、start-local.ps1/.cmd、docker-compose.yml、.env.example 等）已按需求整体移除，旧文件备份在 `%TEMP%\travel-scaffold-backup-20260914-202204`，需要时可以从该目录整体还原。

2026-09-20：明确 `travel-planner/` 转为长期手写维护的业务代码库，不再从 `scaffold-cli/` 模板重新生成；
脚手架与业务各自演进，禁止对 `travel-planner/` 执行 `--force` 重新生成（详见第六节）。

2026-09-22：接口契约落成单一可信源。后端 DTO 经 springdoc 输出 `/v3/api-docs`，由新增的
`OpenApiContractTest` 固化成 `travel-planner/docs/openapi.json`；前端用零依赖脚本
`npm run gen:api` 从契约生成 `src/api/schema.ts`，原来的手写类型文件 `src/api/types.ts` 已删除。
CI 的前端 job 增加了契约同步校验（详见 6.6 节）。

2026-09-22（规范补齐）：根目录加 `.editorconfig`；新增 `AGENTS.md` 作为规则入口，
以及 `.github/CODEOWNERS` 与 PR 模板；错误码落成 `travel-planner/docs/error-codes.md`
（`ErrorCode` + `BusinessException` + `api/GlobalExceptionHandler`）；
前端把 Prettier 接进 CI（`format:check`），并把排版权收归 Prettier、关掉两条会互相打架的 vue 规则；
`scaffold-cli` 本体也挂上 Checkstyle（`mvn -Pquality verify`），与它生成的模板用同一份规则。

`.run/` 保留下来并指向新骨架：`backend: spring-boot:run`（默认 8080）、`backend: mvn test`、`backend: quality (checkstyle)`、`frontend: npm run dev`（默认 5173），用 IDEA 打开仓库根目录即可直接点运行。
