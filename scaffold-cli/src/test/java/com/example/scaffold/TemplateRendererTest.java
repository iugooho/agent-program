package com.example.scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.scaffold.core.TemplateRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

class TemplateRendererTest {

    private final Map<String, String> values = Map.of("packagePath", "com/acme/demo", "name", "demo");

    @Test
    @DisplayName("替换已知占位符、保留未知占位符")
    void render() {
        assertEquals("demo / com/acme/demo", TemplateRenderer.render("{{name}} / {{packagePath}}", values));
        assertEquals("{{unknown}}", TemplateRenderer.render("{{unknown}}", values));
        assertEquals("ademob", TemplateRenderer.render("a{{ name }}b", values));
    }

    @Test
    @DisplayName("路径渲染会替换目录名并去掉 .tpl 后缀")
    void renderPath() {
        Path path = TemplateRenderer.renderPath(Path.of("src/main/java/{{packagePath}}/App.java.tpl"), values);
        assertEquals(Path.of("src/main/java/com/acme/demo/App.java"), path);
    }

    @Test
    @DisplayName("能识别渲染后残留的占位符")
    void unknownPlaceholders() {
        Set<String> unknown = TemplateRenderer.unknownPlaceholders("{{a}} and {{b}}");
        assertEquals(Set.of("a", "b"), unknown);
        assertTrue(TemplateRenderer.unknownPlaceholders("nothing").isEmpty());
    }
}
