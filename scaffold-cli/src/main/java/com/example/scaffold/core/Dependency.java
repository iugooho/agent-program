package com.example.scaffold.core;

/**
 * 一个可选的 Java 依赖。
 *
 * @param propertyName  版本属性名，null 表示不写入 properties 段
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

    /** 渲染成 pom.xml 的 dependency 片段。 */
    public String toDependencyXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("    <dependency>\n");
        xml.append("      <groupId>").append(groupId).append("</groupId>\n");
        xml.append("      <artifactId>").append(artifactId).append("</artifactId>\n");
        xml.append("      <version>").append(versionExpression).append("</version>\n");
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
