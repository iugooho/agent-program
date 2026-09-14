package com.example.scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("默认 Java 依赖为 spring-core / junit / logback")
    void defaults() {
        assertEquals(List.of("spring-core", "junit", "logback"), DependencyCatalog.defaults());
        List<Dependency> dependencies = DependencyCatalog.resolve(DependencyCatalog.defaults());
        assertEquals(3, dependencies.size());

        String xml = DependencyCatalog.renderDependencies(dependencies);
        assertTrue(xml.contains("<artifactId>spring-core</artifactId>"));
        assertTrue(xml.contains("<scope>test</scope>"), "junit 必须是 test 作用域");
        assertTrue(xml.contains("<artifactId>logback-classic</artifactId>"));

        String properties = DependencyCatalog.renderProperties(dependencies);
        assertEquals(2, properties.split("spring.version", -1).length - 1, "版本属性不应重复输出");
    }

    @Test
    @DisplayName("未知依赖给出可选列表")
    void unknownDependency() {
        ScaffoldException error = assertThrows(ScaffoldException.class,
                () -> DependencyCatalog.resolve(List.of("spring-boot-starter-web")));
        assertTrue(error.getMessage().contains("spring-core"), "错误信息应列出可选依赖");
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
    @DisplayName("前端默认依赖为 vue / vue-router，工具链固定附带")
    void frontendDefaults() {
        assertEquals(List.of("vue", "vue-router"), FrontendDependencyCatalog.defaults());

        List<FrontendDependency> runtime = new ArrayList<>();
        for (String id : FrontendDependencyCatalog.defaults()) {
            runtime.addAll(FrontendDependencyCatalog.resolve(List.of(id)));
        }
        String runtimeJson = FrontendDependencyCatalog.renderRuntime(runtime);
        assertTrue(runtimeJson.contains("\"vue\": \"^3.5.13\""));
        assertTrue(runtimeJson.contains("\"vue-router\": \"^4.5.0\""));

        String toolchainJson = FrontendDependencyCatalog.renderToolchain(FrontendDependencyCatalog.toolchain());
        assertTrue(toolchainJson.contains("\"vite\""));
        assertTrue(toolchainJson.contains("\"typescript\""));
        assertTrue(toolchainJson.contains("\"eslint\""));
        assertThrows(ScaffoldException.class, () -> FrontendDependencyCatalog.resolve(List.of("element-plus")));
    }
}
