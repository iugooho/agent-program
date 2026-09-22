package com.example.scaffold;

import com.example.scaffold.cli.CliParser;
import com.example.scaffold.cli.ConsolePrompter;
import com.example.scaffold.cli.ParsedCommand;
import com.example.scaffold.core.DependencyCatalog;
import com.example.scaffold.core.FrontendDependencyCatalog;
import com.example.scaffold.core.GeneratedProject;
import com.example.scaffold.core.ProjectType;
import com.example.scaffold.core.ScaffoldEngine;
import com.example.scaffold.core.ScaffoldException;
import com.example.scaffold.core.ScaffoldRequest;
import com.example.scaffold.core.TemplateResolver;

import java.nio.file.Path;

/** 脚手架生成器入口。 */
public final class ScaffoldCli {

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            ParsedCommand command = new CliParser().parse(args);
            return switch (command.mode()) {
                case HELP -> {
                    System.out.println(CliParser.helpText());
                    yield 0;
                }
                case LIST -> {
                    printCatalog();
                    yield 0;
                }
                case INIT -> generate(command.request());
            };
        } catch (ScaffoldException e) {
            System.err.println("错误: " + e.getMessage());
            return 2;
        }
    }

    private static int generate(ScaffoldRequest initial) {
        ScaffoldRequest request = new ConsolePrompter().complete(initial);
        GeneratedProject project = new ScaffoldEngine(new TemplateResolver(request.templatesDir())).generate(request);
        printReport(request, project);
        return 0;
    }

    private static void printCatalog() {
        System.out.println("项目类型:");
        for (ProjectType type : ProjectType.values()) {
            System.out.printf("  %-10s %s%n", type.id(), type.label());
        }
        System.out.println("Java 依赖（默认 " + String.join(",", DependencyCatalog.defaults()) + "）:");
        for (String id : DependencyCatalog.ids()) {
            System.out.printf("  %-16s%n", id);
        }
        System.out.println("前端依赖（默认 " + String.join(",", FrontendDependencyCatalog.defaults()) + "）:");
        for (String id : FrontendDependencyCatalog.ids()) {
            System.out.printf("  %-16s%n", id);
        }
        System.out.println("构建工具链（自动附带）: "
                + String.join(", ", FrontendDependencyCatalog.toolchain().stream()
                .map(d -> d.packageName()).toList()));
    }

    private static void printReport(ScaffoldRequest request, GeneratedProject project) {
        String action = project.dryRun() ? "将要生成" : "已生成";
        System.out.println();
        System.out.println(action + " " + request.type().label() + " 项目: " + project.root());
        System.out.println("文件清单 (" + project.files().size() + " 个):");
        project.files().stream().map(Path::toString).sorted().forEach(path -> System.out.println("  " + path));

        if (!project.warnings().isEmpty()) {
            System.out.println();
            project.warnings().forEach(warning -> System.out.println("提示: " + warning));
        }
        if (!project.unknownPlaceholders().isEmpty()) {
            System.out.println();
            System.out.println("警告: 以下占位符没有对应取值，请检查模板: " + project.unknownPlaceholders());
        }
        if (project.dryRun()) {
            System.out.println();
            System.out.println("（dry-run 模式，未写入任何文件）");
            return;
        }

        System.out.println();
        System.out.println("下一步:");
        String dir = request.projectName();
        switch (request.type()) {
            case BACKEND -> {
                System.out.println("  cd " + dir);
                System.out.println("  mvn compile          # 编译");
                System.out.println("  mvn test             # 运行单元测试");
                System.out.println("  mvn exec:java        # 运行 App");
                System.out.println("  mvn -Pquality verify # 代码规范检查");
            }
            case FRONTEND -> {
                System.out.println("  cd " + dir);
                System.out.println("  npm install          # 安装依赖");
                System.out.println("  npm run dev          # 启动开发服务器");
                System.out.println("  npm run build        # 生产构建");
                System.out.println("  npm run lint         # 代码规范检查");
            }
            case FULLSTACK -> {
                System.out.println("  cd " + dir);
                System.out.println("  mvn -f backend/pom.xml test   # 后端编译 + 测试");
                System.out.println("  cd frontend && npm install && npm run dev   # 前端开发服务器");
                System.out.println("  .\\scripts\\dev.ps1            # 一键起后端与前端");
            }
            default -> throw new ScaffoldException("未处理的项目类型: " + request.type());
        }
    }
}
