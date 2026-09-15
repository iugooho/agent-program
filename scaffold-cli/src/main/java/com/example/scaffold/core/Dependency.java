package com.example.scaffold.core;

/**
 * 一个可选的 Java 依赖。
 *
 * @param propertyName  版本属性名，null 表示不写入 properties 段
 * @param versionExpression 版本表达式；留空表示版本交给 Spring Boot BOM 统一管理
 */
public record Dependency(
        String id,
        String groupId,
        String artifactId,
        String propertyName,
        String propertyValue,
        String versionExpression,
        String scope,
        String description) {

    /** 版本由依赖管理（BOM）统一决定时，dependency 里不写 version。 */
    public boolean versionManaged() {
        return versionExpression == null || versionExpression.isBlank();
    }

    /** 是否是 Spring Boot 官方 starter：改用 Log4j2 时需要排除它默认带的 Logback 绑定。 */
    public boolean springBootStarter() {
        return artifactId.startsWith("spring-boot-starter-") && !artifactId.contains("log4j2");
    }

    /** 渲染成 pom.xml 的 dependency 片段。 */
    public String toDependencyXml() {
        return toDependencyXml(false);
    }

    /**
     * 渲染成 pom.xml 的 dependency 片段。
     *
     * @param excludeSpringBootLogging 是否排除 starter 默认携带的日志实现
     * @return dependency 片段
     */
    public String toDependencyXml(boolean excludeSpringBootLogging) {
        StringBuilder xml = new StringBuilder();
        xml.append("    <dependency>\n");
        xml.append("      <groupId>").append(groupId).append("</groupId>\n");
        xml.append("      <artifactId>").append(artifactId).append("</artifactId>\n");
        if (!versionManaged()) {
            xml.append("      <version>").append(versionExpression).append("</version>\n");
        }
        if (excludeSpringBootLogging && springBootStarter()) {
            xml.append("      <exclusions>\n");
            xml.append("        <exclusion>\n");
            xml.append("          <groupId>org.springframework.boot</groupId>\n");
            xml.append("          <artifactId>spring-boot-starter-logging</artifactId>\n");
            xml.append("        </exclusion>\n");
            xml.append("      </exclusions>\n");
        }
        if (scope != null && !scope.isBlank()) {
            xml.append("      <scope>").append(scope).append("</scope>\n");
        }
        xml.append("    </dependency>\n");
        return xml.toString();
    }

    /** 渲染成 properties 里的版本变量，供 pom 复用（不重复输出同名属性）。 */
    public String toPropertyXml() {
        if (propertyName == null || propertyName.isBlank()) {
            return "";
        }
        return "    <" + propertyName + ">" + propertyValue + "</" + propertyName + ">\n";
    }
}
