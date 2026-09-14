# travel-planner

由 `scaffold-cli` 生成的 Java 后端骨架（Maven + JDK 21）。

## 快速开始

```bash
mvn compile          # 编译
mvn test             # 运行单元测试
mvn exec:java        # 运行 App
mvn -Pquality verify # 代码规范检查（Checkstyle）
```

Windows 下也可以直接用 `scripts\run.cmd`（或 `scripts\run.ps1`）编译并运行。

## 目录结构

```
.
├── pom.xml                                  # 依赖与构建配置（编码 / JDK 版本 / 插件）
├── config/checkstyle.xml                    # 团队统一的 Java 代码校验规则
├── scripts/                                 # 常用命令封装
└── src
    ├── main/java/com/travelagent/travelplanner/App.java    # 入口类
    ├── main/resources/logback.xml           # 日志配置
    └── test/java/com/travelagent/travelplanner/AppTest.java
```

## 依赖

当前已引入：spring-core, spring-context, junit-jupiter, logback-classic, slf4j-api, jackson-databind

新增依赖时改 `pom.xml`，版本号统一写在 `<properties>` 里，不要在 dependency 里硬编码多份版本。

## 团队规范

- 源码与资源文件统一 UTF-8、LF 换行（见 `.editorconfig`）
- 编译目标 JDK 21，编译参数带 `-parameters -Xlint:all`
- `mvn -Pquality verify` 会按 `config/checkstyle.xml` 检查代码（`verify` 阶段执行）
- 生成信息记录在项目根目录的 `scaffold.json` 里，便于追溯初始结构
