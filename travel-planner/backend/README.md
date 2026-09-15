# travel-planner

由 `scaffold-cli` 生成的 Java 后端骨架（Spring Boot 3 + Maven + JDK 21），
目录结构、依赖版本和代码规范已在脚手架里统一定好。

## 快速开始

```bash
mvn compile             # 编译
mvn test                # 单元测试（脚手架自带冒烟测试）
mvn spring-boot:run    # 启动服务
mvn -Pquality verify    # 代码规范检查（Checkstyle）
```

健康检查接口：GET /api/v1/health（默认端口 8080）。

## 目录结构

```
.
├── pom.xml                    # 依赖与构建配置（编码 / JDK 版本 / 插件）
├── config/checkstyle.xml      # 团队统一的 Java 代码校验规则
├── scripts/                   # 常用命令封装
└── src
    ├── main/java/com/travelagent/travelplanner/
    │   ├── App.java            # 启动类（Spring Boot 应用入口）
    │   └── api/                # REST 接口与统一响应结构
    ├── main/resources/         # application.yml、日志配置、数据库迁移脚本
    └── test/java/com/travelagent/travelplanner/
```

建议的分层依赖方向：`api → application → domain`，`infrastructure` 实现 `domain` 定义的接口。

## 依赖

当前已引入：spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-actuator, mybatis-plus-spring-boot3-starter, flyway-core, postgresql, h2, springdoc-openapi-starter-webmvc-ui, spring-boot-starter-websocket, spring-boot-starter-test, logback-classic, flyway-database-postgresql, mybatis-plus-jsqlparser

- Spring Boot 官方依赖的版本由 `spring-boot-dependencies`（BOM 3.5.16）统一管理，dependency 里不写版本号。
- 第三方依赖（MyBatis-Plus、SpringDoc、Druid）的版本集中写在 `<properties>` 里，不要在 dependency 中重复写。

## 团队规范

- 源码与资源文件统一 UTF-8、LF 换行（见 `.editorconfig`）。
- 编译目标 JDK 21，编译参数固定带 `-parameters -Xlint:all`。
- `mvn -Pquality verify` 按 `config/checkstyle.xml` 检查代码，在 `verify` 阶段执行。
- 接口一律返回统一响应结构 `ApiResponse`（code / message / data），参数校验、异常处理、日志脱敏在接口层完成。
- 数据库表结构变更一律走迁移脚本，不要手工改库。
- 生成信息记录在项目根目录的 `scaffold.json` 里，便于追溯初始结构。

## 开发指南：新增一个模块

按业务能力分包，一个模块一个包，包内再分四层（以 `itinerary` 为例）：

```
src/main/java/com/travelagent/travelplanner/itinerary/
├── api/                ItineraryController + 请求/响应 DTO（返回 ApiResponse，入参加 @Valid）
├── application/        ItineraryService：用例编排与事务边界
├── domain/             Itinerary、DayPlan 实体 + ItineraryRepository 接口（不写 SQL、不加框架注解）
└── infrastructure/     ItineraryRepositoryImpl、MyBatis-Plus Mapper、外部 API 客户端
```

新增步骤：

1. 先定 `domain` 的实体和仓储接口，再写 `application` 的用例，最后才写 `api` 与 `infrastructure`，保证依赖方向单向。
2. 建表 / 改表都写 `src/main/resources/db/migration/V<编号>__<描述>.sql`，编号递增，**已执行过的脚本不要改**，也不要手工改库。
3. Controller 统一返回 `ApiResponse`，异常交给全局异常处理，不要在方法里手拼响应结构。
4. 测试放 `src/test/java/com/travelagent/travelplanner/<模块>/`，和被测类同包；接口测试用 `@WebMvcTest` + MockMvc（不绑端口，CI 里可跑）。
5. 配置项加到 `application.yml`，环境相关值用 `${ENV_VAR:默认值}` 形式。

跨模块共用的东西放 `com/travelagent/travelplanner/common/`，模块之间不要直接引用对方的 `infrastructure`。
