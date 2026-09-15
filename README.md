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

仓库里有两块东西，改哪块、往哪放完全不一样：

| 目录 | 定位 | 改动方式 |
| --- | --- | --- |
| `scaffold-cli/` | 脚手架本体：Java 代码 + `templates/` 模板 | 加依赖改 catalog，加功能改模板 + `ScaffoldEngine#mounts` |
| `travel-planner/` | 生成出来的业务骨架 | 当成产物：改模板后重新生成，不要在这里长期手写业务代码 |

### 6.1 新增一个可选依赖（脚手架侧）

1. 在 `scaffold-cli/src/main/java/com/example/scaffold/core/DependencyCatalog.java` 的 static 块 `register(new Dependency(...))` 加一行；前端依赖加在 `FrontendDependencyCatalog.java`。
2. 依赖之间有约束（日志实现互斥、需要配套模块、需要默认驱动）就在 `normalize()` 里补规则，并用 `warnings` 回写提示。
3. 这个依赖还需要附带代码时，新建 `scaffold-cli/templates/<模板名>/`，在 `ScaffoldEngine#mounts` 里按依赖条件挂载，必要时声明 `excludedPaths`。
4. 在 `DependencyCatalogTest` / `ScaffoldEngineTest` 里补断言，然后 `mvn -B clean test`。

### 6.2 新增一种项目类型（脚手架侧）

`ProjectType` 加枚举值 → 新建 `templates/<类型>/` → 在 `ScaffoldEngine#mounts` 里登记目标子目录与挂载条件 → 补测试。

### 6.3 新增业务模块（骨架侧）

目录约定写在生成出来的项目 README 里：后端模块分包与四层结构、Controller/DTO 放哪、迁移脚本怎么编号、前端页面与接口封装放哪，见
[travel-planner/README.md](travel-planner/README.md) 的「开发指南：新增一个模块放哪里」，以及
[travel-planner/backend/README.md](travel-planner/backend/README.md)、[travel-planner/frontend/README.md](travel-planner/frontend/README.md)。

### 6.4 模板改动的两条铁律

1. 改完 `scaffold-cli/templates/` 必须 `mvn -B clean package`：`clean` 会清掉 `target/templates`，否则生成器可能还在用旧模板。
2. 业务骨架不要手改（会被下次生成覆盖）：改模板 → 重新 `.\scaffold.ps1 init travel-planner ... --force` → 再跑一遍校验。

### 6.5 提交前自检

```powershell
cd scaffold-cli; mvn -B clean test                              # 生成器自测：24 个用例
mvn -B -f ..\travel-planner\backend\pom.xml -Pquality verify    # 后端：测试 + Checkstyle
npm --prefix ..\travel-planner\frontend run lint                # 前端：ESLint
npm --prefix ..\travel-planner\frontend run test                # 前端：Vitest
npm --prefix ..\travel-planner\frontend run build               # 前端：类型检查 + 生产构建
```

## 七、变更说明

仓库里原来的手搭多模块旅游工程（travel-* 十个模块、根 pom.xml、start-local.ps1/.cmd、docker-compose.yml、.env.example 等）已按需求整体移除，旧文件备份在 `%TEMP%\travel-scaffold-backup-20260914-202204`，需要时可以从该目录整体还原。

`.run/` 保留下来并指向新骨架：`backend: spring-boot:run`（默认 8080）、`backend: mvn test`、`backend: quality (checkstyle)`、`frontend: npm run dev`（默认 5173），用 IDEA 打开仓库根目录即可直接点运行。
