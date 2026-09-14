package com.example.scaffold.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** 生成结果报告。 */
public record GeneratedProject(
        Path root,
        List<Path> files,
        Set<String> unknownPlaceholders,
        List<String> warnings,
        boolean dryRun) {

    public GeneratedProject {
        files = List.copyOf(files);
        unknownPlaceholders = Set.copyOf(unknownPlaceholders);
        warnings = List.copyOf(warnings);
    }
}
