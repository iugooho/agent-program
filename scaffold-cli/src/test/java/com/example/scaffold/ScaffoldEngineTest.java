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
        return new ScaffoldRequest(type, "my-app", "com.acme", "my-app", null, "1.0.0-SNAPSHOT",
                "脚手架测试项目", 21,
                List.of("spring-core", "junit", "logback"),
                List.of("vue", "vue-router"),
                output, null, force, false, false);
    }

    private ScaffoldEngine engine() {
        return new ScaffoldEngine(new TemplateResolver(null));
    }

    @Test
    @DisplayName("生成 Java 后端骨架：目录、pom、测试类齐全且无残留占位符")
    void generatesBackendProject(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.BACKEND, false));

        assertTrue(Files.exists(project.root().resolve("pom.xml")));
        assertTrue(Files.exists(project.root().resolve("src/main/java/com/acme/myapp/App.java")));
        assertTrue(Files.exists(project.root().resolve("src/test/java/com/acme/myapp/AppTest.java")));
        assertTrue(Files.exists(project.root().resolve("src/main/resources/logback.xml")));
        assertTrue(Files.exists(project.root().resolve("config/checkstyle.xml")));
        assertTrue(Files.exists(project.root().resolve(".editorconfig")));
        assertTrue(Files.exists(project.root().resolve("scaffold.json")));

        String pom = Files.readString(project.root().resolve("pom.xml"), StandardCharsets.UTF_8);
        assertTrue(pom.contains("<artifactId>my-app</artifactId>"), "artifactId 未渲染");
        assertTrue(pom.contains("<artifactId>spring-core</artifactId>"), "应包含 spring-core 依赖");
        assertTrue(pom.contains("<artifactId>junit-jupiter</artifactId>"), "应包含 junit 依赖");
        assertTrue(pom.contains("<maven.compiler.release>21</maven.compiler.release>"), "JDK 版本未渲染");
        assertTrue(pom.contains("<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>"), "编码未统一为 UTF-8");

        assertFalse(Files.exists(project.root().resolve("frontend")), "纯后端项目不应包含前端目录");
        assertNoPlaceholderLeft(project.root());
    }

    @Test
    @DisplayName("生成 Vue 前端骨架：package.json 带上所选依赖与命令")
    void generatesFrontendProject(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.FRONTEND, false));

        Path packageJson = project.root().resolve("package.json");
        assertTrue(Files.exists(packageJson));
        assertTrue(Files.exists(project.root().resolve("tsconfig.json")));
        assertTrue(Files.exists(project.root().resolve("eslint.config.js")));
        assertTrue(Files.exists(project.root().resolve("src/router/index.ts")));

        String json = Files.readString(packageJson, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"vue\": \"^3.5.13\""), "应包含 vue 依赖");
        assertTrue(json.contains("\"vue-router\": \"^4.5.0\""), "应包含 vue-router 依赖");
        assertTrue(json.contains("\"dev\": \"vite\""), "应包含 npm run dev 命令");
        assertTrue(json.contains("\"build\": \"vue-tsc --noEmit && vite build\""), "应包含 npm run build 命令");
        assertTrue(json.contains("\"eslint\""), "构建工具链应包含 eslint");

        String indexHtml = Files.readString(project.root().resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("<title>my-app</title>"), "index.html 标题未渲染");

        assertFalse(Files.exists(project.root().resolve("pom.xml")), "纯前端项目不应包含 pom.xml");
        assertNoPlaceholderLeft(project.root());
    }

    @Test
    @DisplayName("全栈项目：backend/frontend 分目录，artifactId 自动加后缀")
    void generatesFullstackProject(@TempDir Path tempDir) throws IOException {
        var project = engine().generate(request(tempDir, ProjectType.FULLSTACK, false));

        String backendPom = Files.readString(project.root().resolve("backend/pom.xml"), StandardCharsets.UTF_8);
        String frontendJson = Files.readString(project.root().resolve("frontend/package.json"), StandardCharsets.UTF_8);

        assertTrue(backendPom.contains("<artifactId>my-app-backend</artifactId>"));
        assertTrue(frontendJson.contains("\"name\": \"my-app-frontend\""));
        assertTrue(Files.exists(project.root().resolve("README.md")));
        assertTrue(Files.exists(project.root().resolve("scripts/dev.ps1")));
        assertTrue(Files.exists(project.root().resolve("scaffold.json")));

        assertNoPlaceholderLeft(project.root());
    }

    @Test
    @DisplayName("目标目录非空且未加 --force 时拒绝覆盖")
    void refusesToOverwriteWithoutForce(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-app");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("keep.txt"), "existing");

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
