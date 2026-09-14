package com.example.scaffold.core;

/** 一个可选的 npm 依赖。 */
public record FrontendDependency(String id, String packageName, String version, boolean dev, String description) {

    public String toPackageJsonEntry() {
        return "    \"" + packageName + "\": \"" + version + "\"";
    }
}
