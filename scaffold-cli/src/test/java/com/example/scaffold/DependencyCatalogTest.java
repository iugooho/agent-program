package com.example.scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.scaffold.core.Dependency;
import com.example.scaffold.core.DependencyCatalog;
import com.example.scaffold.core.FrontendDependency;
import com.example.scaffold.core.FrontendDependencyCatalog;
import com.example.scaffold.core.ScaffoldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class DependencyCatalogTest {

    @Test
    @DisplayName("默认 Java 依赖为 spring-boot-web / spring-boot-test / logback，版本交给 BOM")
    void defaults() {
        assertEquals(List.of("spring-boot-web", "spring-boot-test", "logback"), DependencyCatalog.defaults());

        List<Dependency> dependencies = DependencyCatalog.resolve(DependencyCatalog.defaults());
        assertEquals(3, dependencies.size());

        String xml = DependencyCatalog.renderDependencies(dependencies);
        assertTrue(xml.contains("<artifactId>spring-boot-starter-web</artifactId>"));
        assertTrue(xml.contains("<artifactId>spring-boot-starter-test</artifactId>"), "应包含 Spring Boot 测试启动器");
        assertTrue(xml.contains("<scope>test</scope>"), "测试依赖必须是 test 作用域");
        assertTrue(xml.contains("<artifactId>logback-classic</artifactId>"));
        assertFalse(xml.contains("<artifactId>spring-boot-starter-web</artifactId>\n      <version>"),
                "官方依赖的版本由 BOM 决定，不应在 dependency 里写 version");

        String properties = DependencyCatalog.renderProperties(dependencies);
        assertTrue(properties.contains("<spring-boot.version>3.5.16</spring-boot.version>"), "应写入 BOM 版本属性");

        String management = DependencyCatalog.renderDependencyManagement(dependencies);
        assertTrue(management.contains("<artifactId>spring-boot-dependencies</artifactId>"));

        assertTrue(DependencyCatalog.isSpringBootApp(dependencies));
        assertTrue(DependencyCatalog.isWebApp(dependencies));
    }

    @Test
    @DisplayName("纯基础依赖不引入 Spring Boot BOM 与插件")
    void nonSpringBootDependencies() {
        List<Dependency> dependencies = DependencyCatalog.resolve(List.of("spring-core", "junit", "logback"));

        assertEquals("", DependencyCatalog.renderDependencyManagement(dependencies));
        assertEquals("", DependencyCatalog.renderSpringBootPlugin(dependencies));
        assertFalse(DependencyCatalog.isSpringBootApp(dependencies));

        String properties = DependencyCatalog.renderProperties(dependencies);
        assertTrue(properties.contains("<spring.version>6.2.19</spring.version>"));
        assertFalse(properties.contains("spring-boot.version"));
    }

    @Test
    @DisplayName("未知依赖给出可选列表")
    void unknownDependency() {
        ScaffoldException error = assertThrows(ScaffoldException.class,
                () -> DependencyCatalog.resolve(List.of("spring-boot-starter-web")));
        assertTrue(error.getMessage().contains("spring-boot-web"), "错误信息应列出可选依赖");
    }

    @Test
    @DisplayName("未选择日志依赖时自动补 slf4j-api")
    void autoAddLoggingApi() {
        List<Dependency> dependencies = DependencyCatalog.resolve(List.of("spring-core", "junit"));
        List<Dependency> effective = DependencyCatalog.ensureLoggingApi(dependencies);
        assertEquals(dependencies.size() + 1, effective.size());
        assertTrue(effective.stream().anyMatch(d -> d.artifactId().equals("slf4j-api")));
    }

    @Test
    @DisplayName("normalize 处理日志互斥、Flyway 数据库支持与缺失的驱动/测试框架")
    void normalizeConstraints() {
        List<String> warnings = new ArrayList<>();
        List<Dependency> log4j2 = DependencyCatalog.normalize(
                DependencyCatalog.resolve(List.of("spring-boot-web", "log4j2", "logback", "junit")), warnings);

        assertTrue(DependencyCatalog.contains(log4j2, "log4j2"));
        assertFalse(DependencyCatalog.contains(log4j2, "logback"), "选了 Log4j2 应移除 Logback");
        assertFalse(warnings.isEmpty(), "依赖被自动调整时应给出提示");
        assertTrue(DependencyCatalog.renderDependencies(log4j2).contains("<artifactId>spring-boot-starter-logging</artifactId>"),
                "应排除 starter 默认携带的日志实现");

        List<Dependency> flyway = DependencyCatalog.normalize(
                DependencyCatalog.resolve(List.of("spring-boot-web", "flyway", "postgresql", "junit")), new ArrayList<>());
        assertTrue(DependencyCatalog.contains(flyway, "flyway-postgresql"), "flyway + postgresql 应补数据库支持");

        List<Dependency> noDriver = DependencyCatalog.normalize(
                DependencyCatalog.resolve(List.of("spring-boot-web", "mybatis-plus", "junit")), new ArrayList<>());
        assertTrue(DependencyCatalog.contains(noDriver, "h2"), "选了持久层但没选驱动时应自动补 H2");
        assertTrue(DependencyCatalog.contains(noDriver, "mybatis-plus-jsqlparser"),
                "MyBatis-Plus 3.5.9 起分页插件单独成模块，应自动补上");

        List<Dependency> noTest = DependencyCatalog.normalize(
                DependencyCatalog.resolve(List.of("spring-boot-web")), new ArrayList<>());
        assertTrue(DependencyCatalog.contains(noTest, "spring-boot-test"),
                "Spring Boot 项目应自动补 spring-boot-starter-test");

        List<Dependency> noTestPlainJava = DependencyCatalog.normalize(
                DependencyCatalog.resolve(List.of("spring-core", "logback")), new ArrayList<>());
        assertTrue(noTestPlainJava.stream().anyMatch(d -> d.artifactId().equals("junit-jupiter")),
                "非 Spring Boot 项目应自动补 junit-jupiter");
    }

    @Test
    @DisplayName("前端默认依赖为 vue / vue-router / pinia / axios，工具链附带 Vitest")
    void frontendDefaults() {
        assertEquals(List.of("vue", "vue-router", "pinia", "axios"), FrontendDependencyCatalog.defaults());

        List<FrontendDependency> runtime = new ArrayList<>();
        for (String id : FrontendDependencyCatalog.defaults()) {
            runtime.addAll(FrontendDependencyCatalog.resolve(List.of(id)));
        }
        String runtimeJson = FrontendDependencyCatalog.renderRuntime(runtime);
        assertTrue(runtimeJson.contains("\"vue\": \"^3.5.13\""));
        assertTrue(runtimeJson.contains("\"axios\": \"^1.7.9\""));

        String toolchainJson = FrontendDependencyCatalog.renderToolchain(FrontendDependencyCatalog.toolchain());
        assertTrue(toolchainJson.contains("\"vite\""));
        assertTrue(toolchainJson.contains("\"eslint\""));
        assertTrue(toolchainJson.contains("\"vitest\""), "工具链应包含 vitest");
        assertTrue(toolchainJson.contains("\"@vue/test-utils\""), "工具链应包含 Vue Test Utils");
        assertThrows(ScaffoldException.class, () -> FrontendDependencyCatalog.resolve(List.of("element-plus")));
    }
}
