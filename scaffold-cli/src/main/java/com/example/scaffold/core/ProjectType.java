package com.example.scaffold.core;

import java.util.Arrays;
import java.util.stream.Collectors;

/** 可生成的项目类型。 */
public enum ProjectType {

    BACKEND("backend", "Java 后端（Maven）"),
    FRONTEND("frontend", "Vue 3 + TypeScript 前端（Vite）"),
    FULLSTACK("fullstack", "全栈：后端 + 前端");

    private final String id;
    private final String label;

    ProjectType(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static ProjectType fromId(String value) {
        for (ProjectType type : values()) {
            if (type.id.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new ScaffoldException("未知项目类型: " + value + "，可选: " + ids());
    }

    public static String ids() {
        return Arrays.stream(values()).map(ProjectType::id).collect(Collectors.joining(" | "));
    }
}
