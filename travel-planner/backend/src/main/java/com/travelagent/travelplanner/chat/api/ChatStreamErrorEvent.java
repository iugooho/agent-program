package com.travelagent.travelplanner.chat.api;

/**
 * SSE 错误事件。
 *
 * @param code 业务错误码
 * @param message 错误提示
 */
public record ChatStreamErrorEvent(int code, String message) {
}
