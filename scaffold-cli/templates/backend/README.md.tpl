# {{projectName}}

由 `scaffold-cli` 生成的 Java 后端骨架（Spring Boot 3 + Maven + JDK {{javaVersion}}），
目录结构、依赖版本和代码规范已在脚手架里统一定好。

## 快速开始

```bash
mvn compile             # 编译
mvn test                # 单元测试（脚手架自带冒烟测试）
{{backendRunCommand}}    # 启动服务
mvn -Pquality verify    # 代码规范检查（Checkstyle）
```

健康检查接口：{{healthEndpoint}}（默认端口 8080）。

## 目录结构

```
.
├── pom.xml                    # 依赖与构建配置（编码 / JDK 版本 / 插件）
├── config/checkstyle.xml      # 团队统一的 Java 代码校验规则
├── scripts/                   # 常用命令封装
└── src
    ├── main/java/{{packagePath}}/
    │   ├── App.java            # 启动类（Spring Boot 应用入口）
    │   └── api/                # REST 接口与统一响应结构
    ├── main/resources/         # application.yml、日志配置、数据库迁移脚本
    └── test/java/{{packagePath}}/
```

建议的分层依赖方向：`api → application → domain`，`infrastructure` 实现 `domain` 定义的接口。

## 依赖

当前已引入：{{javaDependencyIds}}

- Spring Boot 官方依赖的版本由 `spring-boot-dependencies`（BOM {{springBootVersion}}）统一管理，dependency 里不写版本号。
- 第三方依赖（MyBatis-Plus、SpringDoc、Druid）的版本集中写在 `<properties>` 里，不要在 dependency 中重复写。

## 团队规范

- 源码与资源文件统一 UTF-8、LF 换行（见 `.editorconfig`）。
- 编译目标 JDK {{javaVersion}}，编译参数固定带 `-parameters -Xlint:all`。
- `mvn -Pquality verify` 按 `config/checkstyle.xml` 检查代码，在 `verify` 阶段执行。
- 接口一律返回统一响应结构 `ApiResponse`（code / message / data），参数校验、异常处理、日志脱敏在接口层完成。
- 数据库表结构变更一律走迁移脚本，不要手工改库。
- 生成信息记录在项目根目录的 `scaffold.json` 里，便于追溯初始结构。
