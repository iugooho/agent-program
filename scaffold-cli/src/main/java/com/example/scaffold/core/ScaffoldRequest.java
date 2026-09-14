package com.example.scaffold.core;

import java.nio.file.Path;
import java.util.List;

/** 一次生成请求的全部输入。 */
public record ScaffoldRequest(
        ProjectType type,
        String projectName,
        String groupId,
        String artifactId,
        String packageName,
        String version,
        String description,
        int javaVersion,
        List<String> javaDeps,
        List<String> frontendDeps,
        Path outputDir,
        Path templatesDir,
        boolean force,
        boolean dryRun,
        boolean interactive) {

    public ScaffoldRequest {
        javaDeps = List.copyOf(javaDeps);
        frontendDeps = List.copyOf(frontendDeps);
    }

    /** 后端 maven artifactId：全栈时自动加 -backend 后缀。 */
    public String backendArtifactId() {
        return type == ProjectType.FULLSTACK ? artifactId + "-backend" : artifactId;
    }

    /** 前端 package.json 的 name：全栈时自动加 -frontend 后缀。 */
    public String frontendArtifactId() {
        return type == ProjectType.FULLSTACK ? artifactId + "-frontend" : artifactId;
    }
}
