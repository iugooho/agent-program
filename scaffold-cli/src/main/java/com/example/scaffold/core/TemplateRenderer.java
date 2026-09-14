package com.example.scaffold.core;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {{key}} 占位符渲染：同时支持文件内容与文件/目录名。 */
public final class TemplateRenderer {

    public static final String TEMPLATE_SUFFIX = ".tpl";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    private TemplateRenderer() {
    }

    public static boolean isTemplate(Path path) {
        return path.getFileName().toString().endsWith(TEMPLATE_SUFFIX);
    }

    public static String render(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = values.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? matcher.group(0) : replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** 渲染相对路径：逐段替换占位符，并去掉文件名的 .tpl 后缀。 */
    public static Path renderPath(Path relative, Map<String, String> values) {
        Path result = null;
        for (Path segment : relative) {
            String name = render(segment.toString(), values);
            if (name.endsWith(TEMPLATE_SUFFIX)) {
                name = name.substring(0, name.length() - TEMPLATE_SUFFIX.length());
            }
            result = result == null ? Path.of(name) : result.resolve(name);
        }
        if (result == null) {
            throw new ScaffoldException("非法的模板路径: " + relative);
        }
        return result;
    }

    /** 渲染后仍然存在的占位符，通常是模板写错或漏传参数。 */
    public static Set<String> unknownPlaceholders(String rendered) {
        Set<String> unknown = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(rendered);
        while (matcher.find()) {
            unknown.add(matcher.group(1));
        }
        return unknown;
    }
}
