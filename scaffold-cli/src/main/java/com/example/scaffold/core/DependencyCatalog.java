package com.example.scaffold.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 依赖目录：新增一个可选项只需要在 static 块里 register 一行。
 *
 * <p>版本策略：Spring Boot 官方依赖交给 spring-boot-dependencies（BOM）统一管版本，
 * dependency 里不写 version；第三方依赖（MyBatis-Plus、SpringDoc、Druid）在本文件的
 * properties 段集中写版本，避免每个人各写一份。</p>
 */
public final class DependencyCatalog {

    /** Spring Boot BOM 版本：选到任意官方 starter 时自动引入。 */
    public static final String SPRING_BOOT_VERSION = "3.5.16";

    private static final Map<String, Dependency> ALL = new LinkedHashMap<>();

    static {
        // 基础依赖：不引入 Spring Boot，也能单独使用
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

        // Spring Boot 官方 starter：版本由 BOM 管理
        register(new Dependency("spring-boot-web", "org.springframework.boot", "spring-boot-starter-web",
                null, null, "", "compile",
                "Spring Boot Web：REST 接口、内嵌 Tomcat、JSON"));
        register(new Dependency("spring-boot-test", "org.springframework.boot", "spring-boot-starter-test",
                null, null, "", "test",
                "Spring Boot 测试：JUnit 5、AssertJ、Mockito"));
        register(new Dependency("spring-boot-security", "org.springframework.boot", "spring-boot-starter-security",
                null, null, "", "compile",
                "Spring Security：认证与接口授权"));
        register(new Dependency("validation", "org.springframework.boot", "spring-boot-starter-validation",
                null, null, "", "compile",
                "Bean Validation 入参校验"));
        register(new Dependency("actuator", "org.springframework.boot", "spring-boot-starter-actuator",
                null, null, "", "compile",
                "健康检查与运行指标端点"));
        register(new Dependency("redis", "org.springframework.boot", "spring-boot-starter-data-redis",
                null, null, "", "compile",
                "Redis：缓存、限流与会话"));
        register(new Dependency("rabbitmq", "org.springframework.boot", "spring-boot-starter-amqp",
                null, null, "", "compile",
                "RabbitMQ：异步任务与消息队列"));
        register(new Dependency("websocket", "org.springframework.boot", "spring-boot-starter-websocket",
                null, null, "", "compile",
                "WebSocket(STOMP)：实时消息推送"));
        register(new Dependency("log4j2", "org.springframework.boot", "spring-boot-starter-log4j2",
                null, null, "", "compile",
                "Log4j2 日志实现（替换默认 Logback，要求 2.17 以上）"));

        // 持久层与数据库
        register(new Dependency("mybatis-plus", "com.baomidou", "mybatis-plus-spring-boot3-starter",
                "mybatis-plus.version", "3.5.9", "${mybatis-plus.version}", "compile",
                "MyBatis-Plus：单表 CRUD、分页与条件构造器"));
        register(new Dependency("mybatis-plus-jsqlparser", "com.baomidou", "mybatis-plus-jsqlparser",
                "mybatis-plus.version", "3.5.9", "${mybatis-plus.version}", "compile",
                "MyBatis-Plus 分页插件依赖（3.5.9 起分页单独成模块，选 mybatis-plus 时自动引入）"));
        register(new Dependency("flyway", "org.flywaydb", "flyway-core",
                null, null, "", "compile",
                "Flyway：数据库迁移脚本"));
        register(new Dependency("flyway-postgresql", "org.flywaydb", "flyway-database-postgresql",
                null, null, "", "compile",
                "Flyway 的 PostgreSQL 支持（选 flyway + postgresql 时自动引入）"));
        register(new Dependency("postgresql", "org.postgresql", "postgresql",
                null, null, "", "runtime",
                "PostgreSQL JDBC 驱动"));
        register(new Dependency("h2", "com.h2database", "h2",
                null, null, "", "runtime",
                "H2 内存数据库：本地启动与集成测试"));
        register(new Dependency("druid", "com.alibaba", "druid-spring-boot-3-starter",
                "druid.version", "1.2.23", "${druid.version}", "compile",
                "Druid 连接池（替换默认 HikariCP，带 SQL 监控）"));

        // 接口文档
        register(new Dependency("springdoc", "org.springdoc", "springdoc-openapi-starter-webmvc-ui",
                "springdoc.version", "2.8.9", "${springdoc.version}", "compile",
                "SpringDoc OpenAPI：Swagger UI 接口文档"));
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
        return List.of("spring-boot-web", "spring-boot-test", "logback");
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
     * 处理依赖之间的约束，保证生成出来的项目一定能编译、能跑测试。
     *
     * <ol>
     *   <li>Log4j2 与 Logback 互斥，避免出现两个 SLF4J 绑定</li>
     *   <li>选 Flyway + PostgreSQL 时自动补 flyway-database-postgresql</li>
     *   <li>选了持久层却没选驱动时自动补 H2，保证本地能直接启动</li>
     *   <li>没有日志门面时自动补 slf4j-api，没有测试框架时自动补 junit-jupiter</li>
     * </ol>
     *
     * @param dependencies 用户选择的依赖
     * @param warnings    自动调整的说明，回写给用户
     * @return 规范化之后的依赖列表
     */
    public static List<Dependency> normalize(List<Dependency> dependencies, List<String> warnings) {
        List<Dependency> result = new ArrayList<>(dependencies);

        if (contains(result, "log4j2") && contains(result, "logback")) {
            result.removeIf(d -> d.id().equals("logback"));
            warnings.add("Log4j2 与 Logback 只能保留一个，已移除 logback-classic，避免出现两个 SLF4J 绑定");
        }
        if (contains(result, "flyway") && contains(result, "postgresql") && !contains(result, "flyway-postgresql")) {
            result.add(ALL.get("flyway-postgresql"));
        }
        if (contains(result, "mybatis-plus") && !contains(result, "mybatis-plus-jsqlparser")) {
            result.add(ALL.get("mybatis-plus-jsqlparser"));
        }
        boolean needsDriver = contains(result, "mybatis-plus") || contains(result, "flyway");
        if (needsDriver && !contains(result, "h2") && !contains(result, "postgresql")) {
            result.add(ALL.get("h2"));
            warnings.add("选了持久层但没有数据库驱动，已自动加入 H2（runtime），本地可直接启动；生产环境请换成 postgresql");
        }

        List<Dependency> withLogging = ensureLoggingApi(result);
        if (withLogging.size() > result.size()) {
            warnings.add("未选择日志依赖，已自动加入 slf4j-api，保证模板代码可编译");
        }
        List<Dependency> withTests = ensureTestEngine(withLogging);
        if (withTests.size() > withLogging.size()) {
            warnings.add("未选择测试依赖，已自动加入测试框架（Spring Boot 项目为 spring-boot-starter-test），保证 mvn test 开箱可用");
        }
        return withTests;
    }

    public static boolean contains(List<Dependency> dependencies, String id) {
        return dependencies.stream().anyMatch(d -> d.id().equals(id));
    }

    /**
     * 生成的 App.java 使用 SLF4J API；如果用户选的依赖里既没有门面也没有实现，
     * 这里自动补上 slf4j-api，保证生成的项目一定能编译通过。
     */
    public static List<Dependency> ensureLoggingApi(List<Dependency> dependencies) {
        boolean hasLogging = dependencies.stream()
                .anyMatch(d -> d.artifactId().startsWith("slf4j")
                        || d.artifactId().startsWith("logback")
                        || d.artifactId().contains("log4j"));
        if (hasLogging) {
            return dependencies;
        }
        List<Dependency> result = new ArrayList<>(dependencies);
        result.add(ALL.get("slf4j"));
        return result;
    }

    /**
     * 模板自带冒烟测试，没有测试框架时自动补一个，避免生成的项目编译不过。
     *
     * <p>Spring Boot 项目统一用 spring-boot-starter-test（自带 JUnit 5、MockMvc、AssertJ），
     * 非 Spring Boot 项目用 junit-jupiter。</p>
     */
    public static List<Dependency> ensureTestEngine(List<Dependency> dependencies) {
        boolean bootApp = isSpringBootApp(dependencies);
        String required = bootApp ? "spring-boot-test" : "junit";
        boolean present = bootApp
                ? contains(dependencies, "spring-boot-test")
                : dependencies.stream().anyMatch(d -> "test".equals(d.scope()) && d.artifactId().startsWith("junit"));
        if (present) {
            return dependencies;
        }
        List<Dependency> result = new ArrayList<>(dependencies);
        result.add(ALL.get(required));
        return result;
    }

    public static String renderProperties(List<Dependency> dependencies) {
        Set<String> emitted = new LinkedHashSet<>();
        StringBuilder xml = new StringBuilder();
        if (needsSpringBootBom(dependencies)) {
            xml.append("    <spring-boot.version>").append(SPRING_BOOT_VERSION).append("</spring-boot.version>\n");
        }
        for (Dependency dependency : dependencies) {
            if (dependency.propertyName() != null && emitted.add(dependency.propertyName())) {
                xml.append(dependency.toPropertyXml());
            }
        }
        return xml.toString();
    }

    /** 只要选到官方依赖（不带版本号）就需要引入 Spring Boot BOM。 */
    public static boolean needsSpringBootBom(List<Dependency> dependencies) {
        return dependencies.stream().anyMatch(Dependency::versionManaged);
    }

    /** 是否是 Spring Boot 应用：选了非 test 作用域的官方 starter 就按 Spring Boot 骨架生成。 */
    public static boolean isSpringBootApp(List<Dependency> dependencies) {
        return dependencies.stream().anyMatch(d -> d.groupId().startsWith("org.springframework.boot")
                && !"test".equals(d.scope()));
    }

    /** 是否具备 Web 能力：决定是否生成 REST 示例接口与跨域配置。 */
    public static boolean isWebApp(List<Dependency> dependencies) {
        return contains(dependencies, "spring-boot-web");
    }

    /** 渲染 spring-boot-dependencies 的 BOM 导入片段；不需要时返回空字符串。 */
    public static String renderDependencyManagement(List<Dependency> dependencies) {
        if (!needsSpringBootBom(dependencies)) {
            return "";
        }
        return "  <dependencyManagement>\n"
                + "    <dependencies>\n"
                + "      <dependency>\n"
                + "        <groupId>org.springframework.boot</groupId>\n"
                + "        <artifactId>spring-boot-dependencies</artifactId>\n"
                + "        <version>${spring-boot.version}</version>\n"
                + "        <type>pom</type>\n"
                + "        <scope>import</scope>\n"
                + "      </dependency>\n"
                + "    </dependencies>\n"
                + "  </dependencyManagement>\n"
                + "\n";
    }

    /** 渲染 spring-boot-maven-plugin；非 Spring Boot 项目返回空字符串。 */
    public static String renderSpringBootPlugin(List<Dependency> dependencies) {
        if (!isSpringBootApp(dependencies)) {
            return "";
        }
        return "      <plugin>\n"
                + "        <groupId>org.springframework.boot</groupId>\n"
                + "        <artifactId>spring-boot-maven-plugin</artifactId>\n"
                + "        <version>${spring-boot.version}</version>\n"
                + "        <executions>\n"
                + "          <execution>\n"
                + "            <goals>\n"
                + "              <goal>repackage</goal>\n"
                + "            </goals>\n"
                + "          </execution>\n"
                + "        </executions>\n"
                + "      </plugin>\n";
    }

    public static String renderDependencies(List<Dependency> dependencies) {
        boolean useLog4j2 = contains(dependencies, "log4j2");
        StringBuilder xml = new StringBuilder();
        for (Dependency dependency : dependencies) {
            xml.append(dependency.toDependencyXml(useLog4j2));
        }
        return xml.toString();
    }
}
