package com.example.scaffold;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.scaffold.core.ProjectType;
import com.example.scaffold.core.ScaffoldEngine;
import com.example.scaffold.core.ScaffoldException;
import com.example.scaffold.core.ScaffoldRequest;
import com.example.scaffold.core.TemplateResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

class ScaffoldEngineTest {

    private ScaffoldRequest request(Path output, ProjectType type, boolean force) {
        return request(output, type, force,
                List.of("spring-boot-web", "junit", "logback"),
                List.of("vue", "vue-router", "pinia", "axios"));
    }

    private ScaffoldRequest request(Path output, ProjectType type, boolean force,
                                    List<String> javaDeps, List<String> frontendDeps) {
        return new ScaffoldRequest(type, "my-app", "com.acme", "my-app", null, "1.0.0-SNAPSHOT",
                "脚手架测试项目", 21, javaDeps, frontendDeps, output, null, force, false, false);
    }

    private ScaffoldEngine engine() {
        return new ScaffoldEngine(new TemplateResolver(null));
    }

    private String read(Path root, String relative) throws IOException {
        return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Spring Boot 后端：启动类、REST 示例、统一响应、BOM 版本管理齐全")
    void generatesSpringBootBackend(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.BACKEND, false));
        Path root = project.root();

        assertTrue(Files.exists(root.resolve("pom.xml")));
        assertTrue(Files.exists(root.resolve("src/main/java/com/acme/myapp/App.java")));
        assertTrue(Files.exists(root.resolve("src/main/java/com/acme/myapp/api/ApiResponse.java")));
        assertTrue(Files.exists(root.resolve("src/main/java/com/acme/myapp/api/HealthController.java")));
        assertTrue(Files.exists(root.resolve("src/main/java/com/acme/myapp/config/WebCorsConfig.java")));
        assertTrue(Files.exists(root.resolve("src/main/resources/application.yml")));
        assertTrue(Files.exists(root.resolve("src/main/resources/logback.xml")));
        assertTrue(Files.exists(root.resolve("src/test/java/com/acme/myapp/api/ApiResponseTest.java")));
        assertTrue(Files.exists(root.resolve("config/checkstyle.xml")));
        assertTrue(Files.exists(root.resolve("scaffold.json")));

        String pom = read(root, "pom.xml");
        assertTrue(pom.contains("<artifactId>my-app</artifactId>"), "artifactId 未渲染");
        assertTrue(pom.contains("<artifactId>spring-boot-starter-web</artifactId>"), "应包含 Spring Boot Web");
        assertTrue(pom.contains("<artifactId>spring-boot-dependencies</artifactId>"), "应导入 Spring Boot BOM");
        assertTrue(pom.contains("<artifactId>spring-boot-maven-plugin</artifactId>"), "应带 Spring Boot 插件");
        assertTrue(pom.contains("<spring-boot.version>3.5.16</spring-boot.version>"), "BOM 版本未写入 properties");
        assertTrue(pom.contains("<maven.compiler.release>21</maven.compiler.release>"), "JDK 版本未渲染");
        assertTrue(pom.contains("<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>"), "编码未统一");

        String app = read(root, "src/main/java/com/acme/myapp/App.java");
        assertTrue(app.contains("@SpringBootApplication"), "选了 Spring Boot 依赖时启动类应带注解");
        assertTrue(app.contains("SpringApplication.run(App.class, args);"), "启动类应调用 SpringApplication.run");

        assertTrue(read(root, "src/main/resources/application.yml").contains("port: 8080"), "application.yml 端口未渲染");
        assertFalse(Files.exists(root.resolve("frontend")), "纯后端项目不应包含前端目录");
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("只选基础依赖时退化为普通 Java 骨架，不生成 Spring Boot 代码")
    void generatesPlainJavaBackend(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.BACKEND, false,
                List.of("spring-core", "junit", "logback"), List.of("vue", "vue-router")));
        Path root = project.root();

        String pom = read(root, "pom.xml");
        assertTrue(pom.contains("<artifactId>spring-core</artifactId>"));
        assertFalse(pom.contains("spring-boot-dependencies"), "非 Spring Boot 项目不应引入 BOM");
        assertFalse(pom.contains("spring-boot-maven-plugin"), "非 Spring Boot 项目不应带 Boot 插件");

        String app = read(root, "src/main/java/com/acme/myapp/App.java");
        assertFalse(app.contains("@SpringBootApplication"), "没选 Spring Boot 依赖时不应加注解");
        assertFalse(Files.exists(root.resolve("src/main/java/com/acme/myapp/api/HealthController.java")));
        assertFalse(Files.exists(root.resolve("src/main/resources/application.yml")));
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("持久层组合：MyBatis-Plus + Flyway + PostgreSQL 自动补齐数据库支持与迁移脚本")
    void generatesPersistenceStack(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.BACKEND, false,
                List.of("spring-boot-web", "mybatis-plus", "flyway", "postgresql", "junit", "logback"),
                List.of("vue", "vue-router")));
        Path root = project.root();

        String pom = read(root, "pom.xml");
        assertTrue(pom.contains("<artifactId>mybatis-plus-spring-boot3-starter</artifactId>"));
        assertTrue(pom.contains("<artifactId>flyway-core</artifactId>"));
        assertTrue(pom.contains("<artifactId>flyway-database-postgresql</artifactId>"), "flyway + postgresql 应自动补数据库支持");

        assertTrue(Files.exists(root.resolve("src/main/resources/db/migration/V1__init.sql")), "应生成迁移脚本");
        assertTrue(Files.exists(root.resolve("src/main/resources/application-prod.yml")), "应生成 PostgreSQL 数据源配置");
        assertTrue(Files.exists(root.resolve("src/main/java/com/acme/myapp/config/MyBatisPlusConfig.java")));
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("选 websocket 时 STOMP 配置落在包路径目录下，不生成带点的目录名")
    void generatesWebSocketConfigUnderPackagePath(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.BACKEND, false,
                List.of("spring-boot-web", "websocket", "junit", "logback"), List.of("vue")));
        Path root = project.root();

        assertTrue(Files.exists(root.resolve("src/main/java/com/acme/myapp/config/WebSocketConfig.java")),
                "WebSocketConfig 应落在 packagePath 展开后的目录下");
        try (Stream<Path> stream = Files.walk(root)) {
            List<String> dottedDirs = stream.filter(Files::isDirectory)
                    .map(path -> root.relativize(path).toString().replace('\\', '/'))
                    .filter(name -> !name.isEmpty() && name.contains("."))
                    .toList();
            assertTrue(dottedDirs.isEmpty(), "不应生成以包名命名的目录: " + dottedDirs);
        }
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("选 Log4j2 时移除 Logback，只保留一份日志实现")
    void log4j2ReplacesLogback(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.BACKEND, false,
                List.of("spring-boot-web", "log4j2", "junit"), List.of("vue")));
        Path root = project.root();

        String pom = read(root, "pom.xml");
        assertTrue(pom.contains("<artifactId>spring-boot-starter-log4j2</artifactId>"));
        assertTrue(pom.contains("<artifactId>spring-boot-starter-logging</artifactId>"), "starter 应排除默认日志实现");
        assertFalse(pom.contains("<artifactId>logback-classic</artifactId>"), "Log4j2 与 Logback 不能同时存在");
        assertTrue(Files.exists(root.resolve("src/main/resources/log4j2-spring.xml")));
        assertFalse(Files.exists(root.resolve("src/main/resources/logback.xml")), "不应再保留 logback.xml");
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("Vue 前端：依赖、命令、Vitest 工具链与接口封装齐全")
    void generatesFrontendProject(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.FRONTEND, false));
        Path root = project.root();

        assertTrue(Files.exists(root.resolve("package.json")));
        assertTrue(Files.exists(root.resolve("tsconfig.json")));
        assertTrue(Files.exists(root.resolve("eslint.config.js")));
        assertTrue(Files.exists(root.resolve("vitest.config.ts")));
        assertTrue(Files.exists(root.resolve("src/__tests__/App.spec.ts")));
        assertTrue(Files.exists(root.resolve("src/api/http.ts")));
        assertTrue(Files.exists(root.resolve("src/router/index.ts")));

        String json = read(root, "package.json");
        assertTrue(json.contains("\"vue\": \"^3.5.13\""), "应包含 vue 依赖");
        assertTrue(json.contains("\"axios\": \"^1.7.9\""), "应包含 axios 依赖");
        assertTrue(json.contains("\"build\": \"vue-tsc --noEmit && vite build\""), "应包含 npm run build 命令");
        assertTrue(json.contains("\"test\": \"vitest run\""), "应包含 npm run test 命令");
        assertTrue(json.contains("\"vitest\""), "工具链应包含 vitest");

        assertTrue(read(root, "index.html").contains("<title>my-app</title>"), "index.html 标题未渲染");
        assertFalse(Files.exists(root.resolve("pom.xml")), "纯前端项目不应包含 pom.xml");
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("选了 echarts / stomp 时生成图表组件、数据大屏与实时通信封装")
    void generatesFrontendFeatureTemplates(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.FRONTEND, false,
                List.of("spring-boot-web", "junit", "logback"),
                List.of("vue", "vue-router", "echarts", "stomp")));
        Path root = project.root();

        assertTrue(Files.exists(root.resolve("src/components/EChartsPanel.vue")));
        assertTrue(Files.exists(root.resolve("src/views/DashboardView.vue")));
        assertTrue(Files.exists(root.resolve("src/realtime/stompClient.ts")));
        assertTrue(read(root, "src/router/index.ts").contains("/dashboard"), "选了 echarts 时应带数据大屏路由");
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("全栈项目：backend/frontend 分目录，artifactId 自动加后缀")
    void generatesFullstackProject(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.FULLSTACK, false));
        Path root = project.root();

        assertTrue(read(root, "backend/pom.xml").contains("<artifactId>my-app-backend</artifactId>"), "后端 artifactId 未加后缀");
        assertTrue(read(root, "frontend/package.json").contains("\"name\": \"my-app-frontend\""), "前端包名未加后缀");
        assertTrue(read(root, "README.md").contains("my-app-backend"), "根 README 未渲染后端 artifactId");
        assertTrue(Files.exists(root.resolve("scripts/dev.ps1")));
        assertNoPlaceholderLeft(root);
    }

    @Test
    @DisplayName("目标目录非空时拒绝覆盖，--force 时允许")
    void refusesNonEmptyDirectory(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-app");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("keep.txt"), "keep");

        assertThrows(ScaffoldException.class, () -> engine().generate(request(tempDir, ProjectType.BACKEND, false)));
        assertTrue(Files.exists(projectDir.resolve("keep.txt")), "已存在的文件不应被删除");

        engine().generate(request(tempDir, ProjectType.BACKEND, true));
        assertTrue(Files.exists(projectDir.resolve("pom.xml")));
    }

    @Test
    @DisplayName("dry-run 只报告文件，不落盘")
    void dryRunWritesNothing(@TempDir Path tempDir) {
        ScaffoldRequest base = request(tempDir, ProjectType.BACKEND, false);
        ScaffoldRequest dryRun = new ScaffoldRequest(base.type(), base.projectName(), base.groupId(),
                base.artifactId(), base.packageName(), base.version(), base.description(), base.javaVersion(),
                base.javaDeps(), base.frontendDeps(), base.outputDir(), base.templatesDir(), base.force(),
                true, false);
        var project = new ScaffoldEngine(new TemplateResolver(null)).generate(dryRun);

        assertTrue(project.dryRun());
        assertFalse(Files.exists(project.root()), "dry-run 不应创建项目目录");
        assertTrue(project.files().size() > 5, "应列出计划生成的文件");
    }

    private void assertNoPlaceholderLeft(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> files = stream.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                assertFalse(content.contains("{{"), "文件仍存在未替换的占位符: " + file);
            }
        }
    }
}
