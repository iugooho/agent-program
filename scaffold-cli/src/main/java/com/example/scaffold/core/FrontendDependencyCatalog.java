package com.example.scaffold.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前端依赖目录：runtime 依赖可选，构建工具链固定带上，
 * 保证生成出来的前端项目 npm install 后就能 dev / build。
 */
public final class FrontendDependencyCatalog {

    private static final Map<String, FrontendDependency> RUNTIME = new LinkedHashMap<>();
    private static final List<FrontendDependency> TOOLCHAIN = new ArrayList<>();

    static {
        runtime(new FrontendDependency("vue", "vue", "^3.5.13", false, "Vue 3 运行时"));
        runtime(new FrontendDependency("vue-router", "vue-router", "^4.5.0", false, "Vue 官方路由"));
        runtime(new FrontendDependency("pinia", "pinia", "^2.3.0", false, "Vue 官方状态管理"));
        runtime(new FrontendDependency("axios", "axios", "^1.7.9", false, "HTTP 客户端"));

        toolchain(new FrontendDependency("vite", "vite", "^6.0.7", true, "构建与开发服务器"));
        toolchain(new FrontendDependency("plugin-vue", "@vitejs/plugin-vue", "^5.2.1", true, "Vite 的 Vue 插件"));
        toolchain(new FrontendDependency("typescript", "typescript", "~5.7.2", true, "TypeScript 编译器"));
        toolchain(new FrontendDependency("vue-tsc", "vue-tsc", "^2.2.0", true, "Vue 单文件组件类型检查"));
        toolchain(new FrontendDependency("eslint", "eslint", "^9.17.0", true, "ESLint 代码校验"));
        toolchain(new FrontendDependency("eslint-js", "@eslint/js", "^9.17.0", true, "ESLint 官方规则集"));
        toolchain(new FrontendDependency("eslint-plugin-vue", "eslint-plugin-vue", "^9.32.0", true, "Vue 官方 ESLint 规则"));
        toolchain(new FrontendDependency("typescript-eslint", "typescript-eslint", "^8.18.0", true, "TS ESLint 规则"));
        toolchain(new FrontendDependency("prettier", "prettier", "^3.4.2", true, "代码格式化"));
    }

    private FrontendDependencyCatalog() {
    }

    private static void runtime(FrontendDependency dependency) {
        RUNTIME.put(dependency.id(), dependency);
    }

    private static void toolchain(FrontendDependency dependency) {
        TOOLCHAIN.add(dependency);
    }

    public static List<String> ids() {
        return List.copyOf(RUNTIME.keySet());
    }

    public static List<String> defaults() {
        return List.of("vue", "vue-router");
    }

    public static List<FrontendDependency> resolve(List<String> ids) {
        List<FrontendDependency> result = new ArrayList<>();
        for (String raw : ids) {
            String id = raw == null ? "" : raw.trim().toLowerCase();
            if (id.isEmpty() || "none".equals(id)) {
                continue;
            }
            FrontendDependency dependency = RUNTIME.get(id);
            if (dependency == null) {
                throw new ScaffoldException("未知的前端依赖: " + raw + "；可选: " + String.join(", ", RUNTIME.keySet()));
            }
            if (!result.contains(dependency)) {
                result.add(dependency);
            }
        }
        return result;
    }

    public static List<FrontendDependency> toolchain() {
        return List.copyOf(TOOLCHAIN);
    }

    public static String renderRuntime(List<FrontendDependency> dependencies) {
        return render(dependencies, false);
    }

    public static String renderToolchain(List<FrontendDependency> dependencies) {
        return render(dependencies, true);
    }

    private static String render(List<FrontendDependency> dependencies, boolean dev) {
        List<String> entries = new ArrayList<>();
        for (FrontendDependency dependency : dependencies) {
            if (dependency.dev() == dev) {
                entries.add(dependency.toPackageJsonEntry());
            }
        }
        return String.join(",\n", entries);
    }
}
