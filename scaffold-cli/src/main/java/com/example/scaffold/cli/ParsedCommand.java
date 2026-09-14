package com.example.scaffold.cli;

import com.example.scaffold.core.ScaffoldRequest;

/** 解析结果：命令模式 + 生成请求（仅 INIT 时有值）。 */
public record ParsedCommand(CommandMode mode, ScaffoldRequest request) {

    public static ParsedCommand help() {
        return new ParsedCommand(CommandMode.HELP, null);
    }

    public static ParsedCommand list() {
        return new ParsedCommand(CommandMode.LIST, null);
    }

    public static ParsedCommand init(ScaffoldRequest request) {
        return new ParsedCommand(CommandMode.INIT, request);
    }
}
