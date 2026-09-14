package com.example.scaffold.cli;

import com.example.scaffold.core.DependencyCatalog;
import com.example.scaffold.core.FrontendDependencyCatalog;
import com.example.scaffold.core.ProjectType;
import com.example.scaffold.core.ScaffoldException;
import com.example.scaffold.core.ScaffoldRequest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 命令行参数解析。 */
public final class CliParser {

    public ParsedCommand parse(String[] args) {
        if (args == null || args.length == 0) {
            return ParsedCommand.help();
        }
        List<String> tokens = new ArrayList<>(Arrays.asList(args));
        String first = tokens.get(0);
        if (isHelp(first)) {
            return ParsedCommand.help();
        }
        if (isList(first)) {
            return ParsedCommand.list();
        }

        String projectName;
        int index;
        if (isInit(first)) {
            if (tokens.size() < 2 || tokens.get(1).startsWith("-")) {
                throw new ScaffoldException("缺少项目名。示例: scaffold init my-app --type backend");
            }
            projectName = tokens.get(1);
            index = 2;
        } else if (first.startsWith("-")) {
            return ParsedCommand.help();
        } else {
            projectName = first;
            index = 1;
        }

        ProjectType type = ProjectType.BACKEND;
        String groupId = "com.example";
        String artifactId = projectName;
        String packageName = null;
        String version = "1.0.0-SNAPSHOT";
        String description = null;
        int javaVersion = 21;
        List<String> javaDeps = null;
        List<String> frontendDeps = null;
        String output = ".";
        Path templatesDir = null;
        boolean force = false;
        boolean dryRun = false;
        boolean interactive = System.console() != null;

        while (index < tokens.size()) {
            String flag = tokens.get(index++);
            switch (flag) {
                case "-t", "--type" -> type = ProjectType.fromId(next(tokens, index++, flag));
                case "-g", "--group" -> groupId = next(tokens, index++, flag);
                case "-a", "--artifact" -> artifactId = next(tokens, index++, flag);
                case "-p", "--package" -> packageName = next(tokens, index++, flag);
                case "-v", "--version" -> version = next(tokens, index++, flag);
                case "--description" -> description = next(tokens, index++, flag);
                case "-j", "--java" -> javaVersion = parseJavaVersion(next(tokens, index++, flag));
                case "-d", "--deps" -> {
                    javaDeps = new ArrayList<>();
                    index = collectList(tokens, index, javaDeps);
                }
                case "-f", "--frontend-deps" -> {
                    frontendDeps = new ArrayList<>();
                    index = collectList(tokens, index, frontendDeps);
                }
                case "-o", "--output" -> output = next(tokens, index++, flag);
                case "--templates" -> templatesDir = Path.of(next(tokens, index++, flag));
                case "--force" -> force = true;
                case "--dry-run" -> dryRun = true;
                case "-i", "--interactive" -> interactive = true;
                case "--no-interactive" -> interactive = false;
                default -> throw new ScaffoldException("未知参数: " + flag);
            }
        }

        String finalDescription = description == null ? projectName + " 项目（由 scaffold-cli 生成）" : description;
        if (javaDeps == null) {
            javaDeps = DependencyCatalog.defaults();
        }
        if (frontendDeps == null) {
            frontendDeps = FrontendDependencyCatalog.defaults();
        }
        ScaffoldRequest request = new ScaffoldRequest(
                type, projectName, groupId, artifactId, packageName, version, finalDescription,
                javaVersion, javaDeps, frontendDeps, Path.of(output), templatesDir, force, dryRun, interactive);
        return ParsedCommand.init(request);
    }

    private static boolean isHelp(String value) {
        return value.equals("help") || value.equals("-h") || value.equals("--help");
    }

    private static boolean isList(String value) {
        return value.equals("list") || value.equals("--list") || value.equals("templates");
    }

    private static boolean isInit(String value) {
        return value.equals("init") || value.equals("create") || value.equals("new");
    }

    private static String next(List<String> tokens, int index, String flag) {
        if (index >= tokens.size()) {
            throw new ScaffoldException("参数 " + flag + " 缺少取值");
        }
        return tokens.get(index);
    }

    private static int parseJavaVersion(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ScaffoldException("Java 版本必须是数字: " + value);
        }
    }

    /**
     * 收集依赖列表：既支持 {@code --deps a,b,c}，也支持 {@code --deps a b c}。
     *
     * <p>PowerShell 会把 {@code a,b,c} 当成数组展开成多个参数，所以两种写法都要吃。</p>
     */
    private static int collectList(List<String> tokens, int index, List<String> target) {
        int cursor = index;
        while (cursor < tokens.size() && !tokens.get(cursor).startsWith("-")) {
            for (String part : tokens.get(cursor++).split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    target.add(trimmed);
                }
            }
        }
        if (target.isEmpty()) {
            target.add("none");
        }
        return cursor;
    }

    public static String helpText() {
        return """
                scaffold-cli —— 统一规范的项目脚手架生成器

                用法:
                  scaffold-cli init <项目名> [选项]
                  scaffold-cli list

                选项:
                  -t, --type <backend|frontend|fullstack>  项目类型，默认 backend
                  -g, --group <groupId>                    Maven groupId，默认 com.example
                  -a, --artifact <artifactId>              默认为项目名
                  -p, --package <packageName>              Java 包名，默认由 groupId + 项目名推导
                  -v, --version <version>                  项目版本，默认 1.0.0-SNAPSHOT
                  -j, --java <17-25>                       Java 版本，默认 21
                  -d, --deps <id,id,...>                   Java 依赖，默认 spring-core,junit,logback
                  -f, --frontend-deps <id,id,...>          前端依赖，默认 vue,vue-router
                  -o, --output <目录>                      输出目录，默认当前目录
                      --templates <目录>                   自定义模板目录
                      --description <说明>                 项目描述
                      --force                              目标目录已存在且非空时覆盖
                      --dry-run                            只打印将要生成的文件，不写盘
                  -i, --interactive                        交互式问答（默认在终端下自动开启）
                      --no-interactive                     关闭交互式问答，全部使用默认值

                可用 Java 依赖:
                """ + "  " + String.join(", ", DependencyCatalog.ids()) + """

                可用前端依赖:
                """ + "  " + String.join(", ", FrontendDependencyCatalog.ids()) + """

                示例:
                  scaffold-cli init my-service --type backend --group com.acme
                  scaffold-cli init my-web --type frontend --frontend-deps vue,vue-router,pinia
                  scaffold-cli init my-platform --type fullstack --deps spring-core,junit,logback,jackson
                """;
    }
}
