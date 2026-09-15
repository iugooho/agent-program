package com.example.scaffold.core;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板目录定位。查找顺序：
 * <ol>
 *   <li>命令行 --templates 指定的目录</li>
 *   <li>系统属性 scaffold.templates（构建/测试时注入）</li>
 *   <li>环境变量 SCAFFOLD_TEMPLATES</li>
 *   <li>jar 包所在目录下的 templates（mvn package 会自动拷贝）</li>
 *   <li>当前目录下的 templates、scaffold-cli/templates</li>
 * </ol>
 */
public final class TemplateResolver {

    /** 模板目录约定：每个类型一个子目录。 */
    public static final List<String> REQUIRED_DIRS = List.of("backend", "frontend");

    private final Path root;
    private final List<Path> searched;

    public TemplateResolver(Path override) {
        List<Path> candidates = new ArrayList<>();
        if (override != null) {
            candidates.add(override);
        }
        String systemProperty = System.getProperty("scaffold.templates");
        if (systemProperty != null && !systemProperty.isBlank()) {
            candidates.add(Path.of(systemProperty));
        }
        String env = System.getenv("SCAFFOLD_TEMPLATES");
        if (env != null && !env.isBlank()) {
            candidates.add(Path.of(env));
        }
        Path codeSource = codeSourceDirectory();
        if (codeSource != null) {
            candidates.add(codeSource.resolve("templates"));
        }
        candidates.add(Path.of("templates"));
        candidates.add(Path.of("scaffold-cli", "templates"));

        List<Path> normalized = candidates.stream().map(p -> p.toAbsolutePath().normalize()).toList();
        this.searched = normalized;
        this.root = normalized.stream()
                .filter(TemplateResolver::isTemplateRoot)
                .findFirst()
                .orElseThrow(() -> new ScaffoldException(
                        "未找到模板目录（需要包含 backend/ 与 frontend/）。已查找:\n  "
                                + String.join("\n  ", normalized.stream().map(Path::toString).toList())));
    }

    private static boolean isTemplateRoot(Path path) {
        return Files.isDirectory(path)
                && REQUIRED_DIRS.stream().allMatch(dir -> Files.isDirectory(path.resolve(dir)));
    }

    private static Path codeSourceDirectory() {
        try {
            Path location = Path.of(TemplateResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isDirectory(location) ? location : location.getParent();
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }

    public Path root() {
        return root;
    }

    public List<Path> searched() {
        return List.copyOf(searched);
    }

    /**
     * 可选模板目录是否存在：条件化挂载（例如只选到 Web 依赖才生成 REST 示例）时使用。
     *
     * @param name 模板目录名，例如 backend-boot
     * @return 目录存在返回 true
     */
    public boolean hasTemplateDir(String name) {
        return Files.isDirectory(root.resolve(name));
    }

    public Path templateDir(String name) {
        Path dir = root.resolve(name);
        if (!Files.isDirectory(dir)) {
            throw new ScaffoldException("模板目录不存在: " + dir);
        }
        return dir;
    }
}
