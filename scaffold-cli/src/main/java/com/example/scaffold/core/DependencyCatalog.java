package com.example.scaffold.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 依赖目录：新增一个可选项只需要在 static 块里 register 一行。
 * 版本号使用当前仓库里已验证可用的稳定版本。
 */
public final class DependencyCatalog {

    private static final Map<String, Dependency> ALL = new LinkedHashMap<>();

    static {
        register(new Dependency("spring-core", "org.springframework", "spring-core",
                "spring.version", "6.2.19", "${spring.version}", "compile",
                "Spring 核心容器（IoC、工具类）"));
        register(new Dependency("spring-context", "org.springframework", "spring-context",
                "spring.version", "6.2.19", "${spring.version}", "compile",
                "Spring 上下文（注解驱动、事件、资源加载）"));
        register(new Dependency("junit", "org.junit.jupiter", "junit-jupiter",
                "junit.version", "5.12.2", "${junit.version}", "test",
                "JUnit 5 单元测试"));
        register(new Dependency("logback", "ch.qos.logback", "logback-classic",
                "logback.version", "1.5.38", "${logback.version}", "compile",
                "Logback 日志实现（自带 SLF4J 绑定）"));
        register(new Dependency("slf4j", "org.slf4j", "slf4j-api",
                "slf4j.version", "2.0.18", "${slf4j.version}", "compile",
                "SLF4J 日志门面"));
        register(new Dependency("jackson", "com.fasterxml.jackson.core", "jackson-databind",
                "jackson.version", "2.21.4", "${jackson.version}", "compile",
                "Jackson JSON 序列化"));
        register(new Dependency("lombok", "org.projectlombok", "lombok",
                "lombok.version", "1.18.36", "${lombok.version}", "provided",
                "Lombok 编译期样板代码生成"));
    }

    private DependencyCatalog() {
    }

    private static void register(Dependency dependency) {
        ALL.put(dependency.id(), dependency);
    }

    public static List<String> ids() {
        return List.copyOf(ALL.keySet());
    }

    public static List<String> defaults() {
        return List.of("spring-core", "junit", "logback");
    }

    public static List<Dependency> resolve(List<String> ids) {
        List<Dependency> result = new ArrayList<>();
        for (String raw : ids) {
            String id = raw == null ? "" : raw.trim().toLowerCase();
            if (id.isEmpty() || "none".equals(id)) {
                continue;
            }
            Dependency dependency = ALL.get(id);
            if (dependency == null) {
                throw new ScaffoldException("未知的 Java 依赖: " + raw + "；可选: " + String.join(", ", ALL.keySet()));
            }
            if (!result.contains(dependency)) {
                result.add(dependency);
            }
        }
        return result;
    }

    /**
     * 生成的 App.java 使用 SLF4J API，如果用户选的依赖里没有日志实现或门面，
     * 这里自动补上 slf4j-api，保证生成的项目一定能编译通过。
     */
    public static List<Dependency> ensureLoggingApi(List<Dependency> dependencies) {
        boolean hasLogging = dependencies.stream()
                .anyMatch(d -> d.artifactId().startsWith("slf4j") || d.artifactId().startsWith("logback"));
        if (hasLogging) {
            return dependencies;
        }
        List<Dependency> result = new ArrayList<>(dependencies);
        result.add(ALL.get("slf4j"));
        return result;
    }

    public static String renderProperties(List<Dependency> dependencies) {
        Set<String> emitted = new LinkedHashSet<>();
        StringBuilder xml = new StringBuilder();
        for (Dependency dependency : dependencies) {
            if (dependency.propertyName() != null && emitted.add(dependency.propertyName())) {
                xml.append(dependency.toPropertyXml());
            }
        }
        return xml.toString();
    }

    public static String renderDependencies(List<Dependency> dependencies) {
        StringBuilder xml = new StringBuilder();
        for (Dependency dependency : dependencies) {
            xml.append(dependency.toDependencyXml());
        }
        return xml.toString();
    }
}
