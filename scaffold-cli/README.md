# scaffold-cli 项目脚手架生成器

一个纯 Java 实现的命令行脚手架生成器：**一条命令生成统一规范的项目骨架**，团队成员新建项目不再各写各的。

支持三种类型（文件数为默认依赖下的实测值）：

| 类型 | 生成内容 | 默认依赖 | 文件数 |
| --- | --- | --- | --- |
| `backend` | Maven 工程（src/main/java、src/main/resources、src/test/java、pom.xml、Checkstyle、EditorConfig、命令脚本；选到 Spring Boot 时附启动类、REST 示例、统一响应与 MockMvc 测试） | spring-boot-web、spring-boot-test、logback | 17 |
| `frontend` | Vue 3 + TypeScript + Vite 工程（package.json、tsconfig.json、ESLint、Prettier、Vitest、路由、代理、接口封装） | vue、vue-router、pinia、axios | 23 |
| `fullstack` | backend/ + frontend/ + 一键启动脚本 | 上面两者 | 45 |

可选依赖共 30 项（`.\scaffold.ps1 list` 可查全量）：Java 24 项 —— 基础 spring-core / spring-context / junit / logback / slf4j / jackson / lombok；官方 starter spring-boot-web / spring-boot-test / spring-boot-security / validation / actuator / redis / rabbitmq / websocket / log4j2；持久层与数据库 mybatis-plus / mybatis-plus-jsqlparser / flyway / flyway-postgresql / postgresql / h2 / druid；接口文档 springdoc。前端 6 项 —— vue / vue-router / pinia / axios / echarts / stomp。

## 快速开始

```powershell
# 首次运行会自动 mvn package
.\scaffold.ps1 list
.\scaffold.ps1 init my-service --type backend --group com.acme
.\scaffold.ps1 init my-api --type backend --deps spring-boot-web,spring-boot-security,mybatis-plus,flyway,postgresql,actuator,springdoc
.\scaffold.ps1 init my-web --type frontend --frontend-deps vue,vue-router,pinia,axios,echarts
.\scaffold.ps1 init my-platform --type fullstack --deps spring-boot-web,mybatis-plus,h2 --frontend-deps vue,vue-router,pinia,axios
```

也可以直接跑 jar：

```bash
mvn -B package
java -jar target/scaffold-cli-1.0.0.jar init my-app --type backend
```

生成后立刻可用：

```bash
cd my-service
mvn test               # 单元测试（脚手架自带冒烟测试 + MockMvc 接口测试）
mvn spring-boot:run    # 启动 Spring Boot 服务（默认 8080）
mvn compile            # 只编译
mvn -Pquality verify   # Checkstyle 代码规范检查
```

```bash
cd my-web
npm install
npm run dev            # 开发服务器
npm run build          # 生产构建
npm run lint           # ESLint + Prettier
npm run test           # Vitest 单元测试
```

## 命令与选项

```
scaffold-cli init <项目名> [选项]
scaffold-cli list
```

| 选项 | 默认值 | 说明 |
| --- | --- | --- |
| -t, --type | backend | backend / frontend / fullstack |
| -g, --group | com.example | Maven groupId |
| -a, --artifact | 项目名 | Maven artifactId |
| -p, --package | 由 groupId + 项目名推导 | Java 包名 |
| -v, --version | 1.0.0-SNAPSHOT | 项目版本 |
| -j, --java | 21 | 编译目标 JDK（17~25） |
| -d, --deps | spring-boot-web,spring-boot-test,logback | Java 依赖，可选值见 scaffold-cli list |
| -f, --frontend-deps | vue,vue-router,pinia,axios | 前端依赖，可选 vue / vue-router / pinia / axios / echarts / stomp |
| -o, --output | 当前目录 | 输出目录 |
| --templates | 自动查找 | 自定义模板目录 |
| --description | 自动生成 | 项目描述 |
| --force | 关闭 | 目标目录非空时覆盖 |
| --dry-run | 关闭 | 只列出将要生成的文件 |
| -i, --interactive | 终端下自动开启 | 交互式问答选择类型与依赖 |
| --no-interactive | 关闭 | 强制不问答，适合脚本与 CI |

不带参数运行会打印帮助；`list` 列出所有可选类型与依赖。在 CI、管道等没有终端的环境里会自动跳过问答、使用默认值。

## 生成的骨架里有什么

以 backend（默认依赖）为例：

```
my-api/
├── pom.xml                              # UTF-8 编码、JDK 21、Spring Boot BOM 统一管版本
├── scaffold.json                        # 记录生成器版本、类型、依赖，便于追溯初始结构
├── .editorconfig / .gitignore
├── config/checkstyle.xml                # Java 代码校验规则
├── scripts/run.ps1|run.cmd              # 编译并运行
└── src
    ├── main/java/com/acme/myapi/App.java                  # @SpringBootApplication 启动类
    ├── main/java/com/acme/myapi/api/ApiResponse.java      # 统一响应 code/message/data
    ├── main/java/com/acme/myapi/api/HealthController.java
    ├── main/java/com/acme/myapi/config/WebCorsConfig.java
    ├── main/resources/application.yml                     # 端口、profile、CORS、actuator
    ├── main/resources/logback.xml
    ├── test/java/com/acme/myapi/AppTest.java
    └── test/java/com/acme/myapi/api/{ApiResponseTest,HealthControllerTest}.java
```

按依赖追加的功能模板：

| 选择了 | 额外生成 |
| --- | --- |
| mybatis-plus | `config/MyBatisPlusConfig.java`（分页插件），并自动补 `mybatis-plus-jsqlparser` |
| flyway | `src/main/resources/db/migration/V1__init.sql`；同时选 postgresql 时自动补 `flyway-database-postgresql` |
| postgresql / h2 | `application-prod.yml` / `application-local.yml`；两者都没选时自动补 H2（runtime），保证本地开箱能启动 |
| spring-boot-security | `config/SecurityConfig.java`（健康检查与接口文档放行，其余需认证） |
| websocket | `config/WebSocketConfig.java`（STOMP，订阅 `/topic`，端点 `/ws`） |
| log4j2 | `log4j2-spring.xml`，并移除 `logback.xml` 与 starter 自带的日志实现，避免两份 SLF4J 绑定 |

frontend 会生成 package.json（含 dev / build / lint / test / format 命令）、tsconfig.json、vite.config.ts（@ 别名 + /api 代理到 8080）、eslint.config.js、.prettierrc.json、vitest.config.ts、src/router、src/views、src/__tests__ 以及 scripts/dev.cmd；选 axios 时附 `src/api/http.ts`、`src/api/health.ts`，选 echarts 时附 `EChartsPanel.vue`、`DashboardView.vue` 与数据大屏路由，选 stomp 时附 `src/realtime/stompClient.ts`。

fullstack 额外生成 backend/、frontend/、根 README.md、统一 .editorconfig 和 scripts/dev.ps1（后端新窗口启动 + 前端开发服务器）。

> 选了持久层却没选数据库驱动、没选测试框架、同时选了 Log4j2 与 Logback 时，生成器会自动纠正并在输出里给出提示，保证生成的项目一定能编译、能跑测试。

## 统一规范做了什么

1. 编码与换行：所有文本文件 UTF-8 + LF，.editorconfig 按语言规定缩进（Java 4 空格、前端 2 空格）。
2. JDK 版本：maven.compiler.release 与 java.version 统一写入 pom.xml，编译参数固定带 -parameters -Xlint:all。
3. 代码校验：后端 mvn -Pquality verify 走 Checkstyle（config/checkstyle.xml）；前端 npm run lint 走 ESLint 9 扁平配置 + Prettier。
4. 依赖治理：Spring Boot 官方依赖交给 spring-boot-dependencies（BOM）管版本，第三方依赖版本集中写在 pom.xml 的 properties 里，依赖通过 catalog 统一注入，避免每人各写一个版本。
5. 命令统一：mvn test / mvn spring-boot:run / npm run dev / npm run build / npm run test 开箱即用，另有 scripts/ 下的封装脚本。
6. 测试开箱可用：后端生成 AppTest、ApiResponseTest 与 HealthControllerTest（MockMvc，不绑定端口），前端生成 Vitest + jsdom 用例，生成完直接是绿的，团队成员不用自己搭测试脚手架。
7. 可追溯：生成的 scaffold.json 记录生成器版本与本次选择的依赖。

## 扩展方式

加一个 Java 依赖：在 src/main/java/com/example/scaffold/core/DependencyCatalog.java 的 static 块里 register(...) 一行，随后即可用 --deps 选择。

加一个前端依赖：在 FrontendDependencyCatalog.java 里 runtime(...) 一行。

加一个功能模板：在 templates/ 下新建目录（例如 backend-cache），在 ScaffoldEngine#mounts 里按依赖条件挂载，并把它登记到依赖目录里。

改模板：直接编辑 templates/ 下的文件。*.tpl 会被渲染并去掉后缀，其它文件原样拷贝；文件与目录名里的 {{key}} 同样会被替换（例如 src/main/java/{{packagePath}}/App.java.tpl）。占位符取值见 ScaffoldEngine#buildValues。

加一种项目类型：在 ProjectType 增加枚举值 + templates/<类型>/ 目录，并在 ScaffoldEngine#mounts 里登记挂载位置。

改完之后的验证顺序：`mvn -B clean test`（生成器自测）→ `mvn -B clean package`（重新打包并刷新 target/templates）→ 用 `init` 生成一个样例项目，跑一遍后端 `mvn test` 与前端 `npm run lint && npm run test`。注意 `clean` 不能省：`target/templates` 是打包时拷贝的副本，不重新打包生成器可能还在用旧模板。

生成出来的项目要怎么继续加业务模块（后端分包、Controller 位置、迁移脚本编号、前端页面位置），见 templates/fullstack/README.md.tpl 里的「开发指南」小节。

## 开发者命令

```bash
mvn -B clean test     # 运行生成器自身的单元测试（当前 24 个）
mvn -B clean package  # 打包，同时把 templates/ 复制到 target/templates
```

改动或重命名 `templates/` 下的文件后，请用 `mvn clean package`（`clean` 会清掉 `target/templates` 里的旧模板，避免新旧文件同时被生成）。

模板目录查找顺序：--templates > 系统属性 scaffold.templates > 环境变量 SCAFFOLD_TEMPLATES > jar 同级 templates/ > 当前目录 templates/。
