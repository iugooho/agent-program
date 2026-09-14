package com.example.scaffold.core;

/** 生成过程中的可预期错误，统一由 CLI 捕获并打印友好提示。 */
public class ScaffoldException extends RuntimeException {

    public ScaffoldException(String message) {
        super(message);
    }

    public ScaffoldException(String message, Throwable cause) {
        super(message, cause);
    }
}
