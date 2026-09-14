package com.example.scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.scaffold.cli.CliParser;
import com.example.scaffold.cli.CommandMode;
import com.example.scaffold.core.ProjectType;
import com.example.scaffold.core.ScaffoldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class CliParserTest {

    private final CliParser parser = new CliParser();

    @Test
    @DisplayName("无参数或 help 输出帮助，list 输出目录")
    void helpAndList() {
        assertEquals(CommandMode.HELP, parser.parse(new String[0]).mode());
        assertEquals(CommandMode.HELP, parser.parse(new String[]{"--help"}).mode());
        assertEquals(CommandMode.LIST, parser.parse(new String[]{"list"}).mode());
    }

    @Test
    @DisplayName("解析完整命令行参数")
    void initOptions() {
        var request = parser.parse(new String[]{
                "init", "my-shop", "--type", "fullstack", "-g", "com.acme", "-j", "23",
                "-d", "spring-core,junit,logback,jackson", "-f", "vue,vue-router,pinia",
                "--output", "out", "--no-interactive"
        }).request();

        assertEquals(ProjectType.FULLSTACK, request.type());
        assertEquals("com.acme", request.groupId());
        assertEquals(23, request.javaVersion());
        assertEquals(List.of("spring-core", "junit", "logback", "jackson"), request.javaDeps());
        assertEquals(List.of("vue", "vue-router", "pinia"), request.frontendDeps());
        assertEquals("my-shop-backend", request.backendArtifactId());
        assertEquals("my-shop-frontend", request.frontendArtifactId());
        assertFalse(request.interactive());
    }

    @Test
    @DisplayName("不带 init 关键字也可用；缺项目名或非法参数报错")
    void implicitInit() {
        assertEquals("demo", parser.parse(new String[]{"demo"}).request().projectName());
        assertThrows(ScaffoldException.class, () -> parser.parse(new String[]{"init"}));
        assertThrows(ScaffoldException.class, () -> parser.parse(new String[]{"init", "demo", "--type", "web"}));
        assertThrows(ScaffoldException.class, () -> parser.parse(new String[]{"init", "demo", "--unknown"}));
    }

    @Test
    @DisplayName("依赖列表同时支持逗号分隔和空格分隔（PowerShell 会展开数组）")
    void dependencyListForms() {
        var comma = parser.parse(new String[]{"init", "demo", "--deps", "spring-core,junit,logback"}).request();
        assertEquals(List.of("spring-core", "junit", "logback"), comma.javaDeps());

        var spaces = parser.parse(new String[]{"init", "demo", "--deps", "spring-core", "junit", "logback",
                "--frontend-deps", "vue", "vue-router", "pinia", "--output", "out"}).request();
        assertEquals(List.of("spring-core", "junit", "logback"), spaces.javaDeps());
        assertEquals(List.of("vue", "vue-router", "pinia"), spaces.frontendDeps());
        assertEquals("out", spaces.outputDir().toString());
    }

    @Test
    @DisplayName("帮助文本包含可用依赖与命令示例")
    void helpText() {
        String text = CliParser.helpText();
        assertTrue(text.contains("scaffold-cli init"));
        assertTrue(text.contains("spring-core"));
        assertTrue(text.contains("vue-router"));
    }
}
