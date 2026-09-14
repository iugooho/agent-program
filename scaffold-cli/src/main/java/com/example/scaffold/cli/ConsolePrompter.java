package com.example.scaffold.cli;

import com.example.scaffold.core.DependencyCatalog;
import com.example.scaffold.core.FrontendDependencyCatalog;
import com.example.scaffold.core.ProjectType;
import com.example.scaffold.core.ScaffoldException;
import com.example.scaffold.core.ScaffoldRequest;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 交互式问答：没有终端（例如 CI、管道）时自动跳过，全部使用传入的默认值。 */
public final class ConsolePrompter {

    public ScaffoldRequest complete(ScaffoldRequest request) {
        Console console = System.console();
        if (!request.interactive() || console == null) {
            return request;
        }

        System.out.println();
        System.out.println("进入交互模式，直接回车即使用 [默认值]。");
        ProjectType type = askType(console, request.type());
        String groupId = ask(console, "groupId", request.groupId());
        String artifactId = ask(console, "artifactId", request.artifactId());
        int javaVersion = askInt(console, "Java 版本", request.javaVersion());
        List<String> javaDeps = askList(console, "Java 依赖（逗号分隔，可选: "
                + String.join(",", DependencyCatalog.ids()) + "）", request.javaDeps());
        DependencyCatalog.resolve(javaDeps);
        List<String> frontendDeps = request.type() == ProjectType.BACKEND
                ? request.frontendDeps()
                : askList(console, "前端依赖（逗号分隔，可选: "
                        + String.join(",", FrontendDependencyCatalog.ids()) + "）", request.frontendDeps());
        FrontendDependencyCatalog.resolve(frontendDeps);

        return new ScaffoldRequest(type, request.projectName(), groupId, artifactId, request.packageName(),
                request.version(), request.description(), javaVersion, javaDeps, frontendDeps,
                request.outputDir(), request.templatesDir(), request.force(), request.dryRun(), request.interactive());
    }

    private ProjectType askType(Console console, ProjectType current) {
        String value = ask(console, "项目类型（backend / frontend / fullstack）", current.id());
        return ProjectType.fromId(value);
    }

    private String ask(Console console, String label, String current) {
        String answer = console.readLine("%s [%s]: ", label, current);
        return answer == null || answer.isBlank() ? current : answer.trim();
    }

    private int askInt(Console console, String label, int current) {
        String value = ask(console, label, String.valueOf(current));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ScaffoldException(label + " 必须是数字: " + value);
        }
    }

    private List<String> askList(Console console, String label, List<String> current) {
        String value = ask(console, label, String.join(",", current));
        List<String> result = new ArrayList<>();
        Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(result::add);
        return result.isEmpty() ? List.of("none") : result;
    }
}
