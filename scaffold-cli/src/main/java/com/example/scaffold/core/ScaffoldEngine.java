package com.example.scaffold.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 脚手架生成引擎：把 templates/ 下的模板渲染成目标项目。
 *
 * <p>约定：{@code *.tpl} 会被渲染并去掉后缀，其它文件原样拷贝；
 * 文件与目录名里的 {@code {{key}}} 同样会被替换。最终项目 = 基础模板
 * （common / backend / frontend）加上按依赖选择的功能模板
 * （backend-boot、backend-mybatis、backend-flyway、frontend-api 等）。</p>
 */
public final class ScaffoldEngine {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final TemplateResolver templates;

    public ScaffoldEngine(TemplateResolver templates) {
        this.templates = templates;
    }

    public GeneratedProject generate(ScaffoldRequest request) {
        validate(request);

        Path projectDir = request.outputDir().toAbsolutePath().normalize().resolve(request.projectName());
        checkTargetDirectory(projectDir, request.force());

        List<String> warnings = new ArrayList<>();
        List<Dependency> requestedDeps = DependencyCatalog.resolve(request.javaDeps());
        List<Dependency> javaDeps = DependencyCatalog.normalize(requestedDeps, warnings);
        List<FrontendDependency> frontendDeps = FrontendDependencyCatalog.resolve(request.frontendDeps());

        Map<String, String> values = buildValues(request, javaDeps, frontendDeps);

        List<Path> planned = new ArrayList<>();
        Set<String> unknown = new LinkedHashSet<>();

        for (Mount mount : mounts(request, javaDeps, frontendDeps)) {
            Path source = templates.templateDir(mount.templateDir());
            Path target = mount.targetSubdir().isEmpty() ? projectDir : projectDir.resolve(mount.targetSubdir());
            renderTree(source, target, projectDir, values, request.dryRun(), planned, unknown, mount.excludedPaths());
        }

        return new GeneratedProject(projectDir, planned, unknown, warnings, request.dryRun());
    }

    private void validate(ScaffoldRequest request) {
        if (request.projectName() == null || request.projectName().isBlank()) {
            throw new ScaffoldException("项目名不能为空");
        }
        if (!request.projectName().matches("[A-Za-z][A-Za-z0-9._-]*")) {
            throw new ScaffoldException("项目名只能以字母开头，且只包含字母、数字、点、下划线和短横线: " + request.projectName());
        }
        if (request.artifactId() == null || !request.artifactId().matches("[A-Za-z][A-Za-z0-9._-]*")) {
            throw new ScaffoldException("artifactId 非法: " + request.artifactId());
        }
        if (request.groupId() == null || !request.groupId().matches("[A-Za-z][A-Za-z0-9._-]*")) {
            throw new ScaffoldException("groupId 非法: " + request.groupId());
        }
        if (request.javaVersion() < 17 || request.javaVersion() > 25) {
            throw new ScaffoldException("Java 版本需在 17~25 之间，当前: " + request.javaVersion());
        }
    }

    private void checkTargetDirectory(Path projectDir, boolean force) {
        if (!Files.exists(projectDir)) {
            return;
        }
        if (!Files.isDirectory(projectDir)) {
            throw new ScaffoldException("目标路径已存在且不是目录: " + projectDir);
        }
        boolean nonEmpty;
        try (Stream<Path> children = Files.list(projectDir)) {
            nonEmpty = children.findAny().isPresent();
        } catch (IOException e) {
            throw new ScaffoldException("无法读取目标目录: " + projectDir, e);
        }
        if (nonEmpty && !force) {
            throw new ScaffoldException("目录已存在且不为空: " + projectDir + "（如需覆盖请加 --force）");
        }
    }

    /**
     * 一次模板挂载。
     *
     * @param templateDir   模板目录名
     * @param targetSubdir  目标子目录，全栈类型下是 backend / frontend
     * @param excludedPaths 需要跳过的模板文件（相对路径，用 / 分隔）
     */
    private record Mount(String templateDir, String targetSubdir, Set<String> excludedPaths) {
    }

    private List<Mount> mounts(ScaffoldRequest request, List<Dependency> javaDeps,
                               List<FrontendDependency> frontendDeps) {
        ProjectType type = request.type();
        String backendTarget = type == ProjectType.FULLSTACK ? "backend" : "";
        String frontendTarget = type == ProjectType.FULLSTACK ? "frontend" : "";

        Set<String> excluded = new LinkedHashSet<>();
        if (DependencyCatalog.contains(javaDeps, "log4j2")) {
            excluded.add("src/main/resources/logback.xml");
        }

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount("common", "", excluded));

        if (type != ProjectType.FRONTEND) {
            mounts.add(new Mount("backend", backendTarget, excluded));
            if (DependencyCatalog.isWebApp(javaDeps)) {
                mounts.add(new Mount("backend-boot", backendTarget, excluded));
            }
            if (DependencyCatalog.contains(javaDeps, "mybatis-plus")) {
                mounts.add(new Mount("backend-mybatis", backendTarget, excluded));
            }
            if (DependencyCatalog.contains(javaDeps, "h2")) {
                mounts.add(new Mount("backend-datasource-h2", backendTarget, excluded));
            }
            if (DependencyCatalog.contains(javaDeps, "postgresql")) {
                mounts.add(new Mount("backend-datasource-postgres", backendTarget, excluded));
            }
            if (DependencyCatalog.contains(javaDeps, "flyway")) {
                mounts.add(new Mount("backend-flyway", backendTarget, excluded));
            }
            if (DependencyCatalog.contains(javaDeps, "spring-boot-security")) {
                mounts.add(new Mount("backend-security", backendTarget, excluded));
            }
            if (DependencyCatalog.contains(javaDeps, "websocket")) {
                mounts.add(new Mount("backend-websocket", backendTarget, excluded));
            }
            if (DependencyCatalog.contains(javaDeps, "log4j2")) {
                mounts.add(new Mount("backend-log4j2", backendTarget, excluded));
            }
        }

        if (type != ProjectType.BACKEND) {
            mounts.add(new Mount("frontend", frontendTarget, excluded));
            if (FrontendDependencyCatalog.contains(frontendDeps, "axios")) {
                mounts.add(new Mount("frontend-api", frontendTarget, excluded));
            }
            if (FrontendDependencyCatalog.contains(frontendDeps, "echarts")) {
                mounts.add(new Mount("frontend-echarts", frontendTarget, excluded));
            }
            if (FrontendDependencyCatalog.contains(frontendDeps, "stomp")) {
                mounts.add(new Mount("frontend-stomp", frontendTarget, excluded));
            }
        }

        if (type == ProjectType.FULLSTACK) {
            mounts.add(new Mount("fullstack", "", excluded));
        }

        return mounts.stream().filter(mount -> templates.hasTemplateDir(mount.templateDir())).toList();
    }

    private void renderTree(Path source, Path target, Path projectDir, Map<String, String> values,
                            boolean dryRun, List<Path> planned, Set<String> unknown, Set<String> excludedPaths) {
        try (Stream<Path> stream = Files.walk(source).sorted(Comparator.comparing(Path::toString))) {
            List<Path> files = stream.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                Path relative = source.relativize(file);
                if (excludedPaths.contains(relative.toString().replace('\\', '/'))) {
                    continue;
                }
                Path renderedRelative = TemplateRenderer.renderPath(relative, values);
                Path destination = target.resolve(renderedRelative);
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (TemplateRenderer.isTemplate(relative)) {
                    content = TemplateRenderer.render(content, values);
                }
                unknown.addAll(TemplateRenderer.unknownPlaceholders(content));
                if (!dryRun) {
                    Path parent = destination.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.writeString(destination, content, StandardCharsets.UTF_8);
                }
                planned.add(projectDir.relativize(destination));
            }
        } catch (IOException e) {
            throw new ScaffoldException("渲染模板失败: " + source, e);
        }
    }

    private Map<String, String> buildValues(ScaffoldRequest request, List<Dependency> javaDeps,
                                            List<FrontendDependency> frontendDeps) {
        Map<String, String> values = new LinkedHashMap<>();
        String packageName = request.packageName() == null || request.packageName().isBlank()
                ? derivePackageName(request)
                : request.packageName();

        boolean bootApp = DependencyCatalog.isSpringBootApp(javaDeps);
        boolean webApp = DependencyCatalog.isWebApp(javaDeps);
        boolean echarts = FrontendDependencyCatalog.contains(frontendDeps, "echarts");
        boolean postgres = DependencyCatalog.contains(javaDeps, "postgresql");

        values.put("projectName", request.projectName());
        values.put("description", request.description() == null ? "" : request.description());
        values.put("type", request.type().id());
        values.put("groupId", request.groupId());
        values.put("artifactId", request.artifactId());
        values.put("backendArtifactId", request.backendArtifactId());
        values.put("frontendArtifactId", request.frontendArtifactId());
        values.put("packageName", packageName);
        values.put("packagePath", packageName.replace('.', '/'));
        values.put("version", request.version());
        values.put("javaVersion", String.valueOf(request.javaVersion()));
        values.put("surefireVersion", "3.5.6");
        values.put("backendDir", "backend");
        values.put("frontendDir", "frontend");
        values.put("springBootVersion", DependencyCatalog.SPRING_BOOT_VERSION);
        values.put("backendRunCommand", bootApp ? "mvn spring-boot:run" : "mvn compile exec:java");
        values.put("healthEndpoint", webApp ? "GET /api/v1/health" : "未选择 spring-boot-web，暂未生成接口");
        values.put("mybatisDbType", postgres ? "POSTGRE_SQL" : "H2");
        values.put("dependencyProperties", DependencyCatalog.renderProperties(javaDeps));
        values.put("dependencies", DependencyCatalog.renderDependencies(javaDeps));
        values.put("dependencyManagement", DependencyCatalog.renderDependencyManagement(javaDeps));
        values.put("springBootPlugin", DependencyCatalog.renderSpringBootPlugin(javaDeps));
        values.put("appImports", bootApp
                ? "import org.springframework.boot.SpringApplication;\n"
                        + "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n"
                : "");
        values.put("appAnnotations", bootApp ? "@SpringBootApplication\n" : "");
        values.put("appMainBody", bootApp
                ? "SpringApplication.run(App.class, args);\n        log.info(greeting());"
                : "log.info(greeting());\n        System.out.println(greeting());");
        values.put("javaDependencyIds", String.join(", ", javaDeps.stream().map(Dependency::artifactId).toList()));
        values.put("javaDependenciesJson",
                jsonArray(javaDeps.stream().map(d -> d.groupId() + ":" + d.artifactId()).toList()));
        values.put("frontendDependencies", FrontendDependencyCatalog.renderRuntime(frontendDeps));
        values.put("frontendDevDependencies",
                FrontendDependencyCatalog.renderToolchain(FrontendDependencyCatalog.toolchain()));
        values.put("frontendDependencyIds", String.join(", ",
                frontendDeps.stream().map(FrontendDependency::packageName).toList()));
        values.put("frontendDependenciesJson",
                jsonArray(frontendDeps.stream().map(FrontendDependency::packageName).toList()));
        values.put("dashboardRoute", echarts
                ? ",\n  {\n    path: '/dashboard',\n    name: 'dashboard',\n"
                        + "    component: () => import('@/views/DashboardView.vue')\n  }\n"
                : "");
        values.put("dashboardNavLink", echarts
                ? "\n      <RouterLink to=\"/dashboard\">\n        数据大屏\n      </RouterLink>"
                : "");
        values.put("generatorVersion", generatorVersion());
        values.put("generatedAt", OffsetDateTime.now().format(TIMESTAMP));
        return values;
    }

    private static String jsonArray(List<String> items) {
        return String.join(", ", items.stream().map(item -> "\"" + item + "\"").toList());
    }

    private static String derivePackageName(ScaffoldRequest request) {
        String suffix = request.artifactId().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return request.groupId() + "." + suffix;
    }

    private static String generatorVersion() {
        Package pkg = ScaffoldEngine.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return version == null ? "1.0.0-dev" : version;
    }
}
