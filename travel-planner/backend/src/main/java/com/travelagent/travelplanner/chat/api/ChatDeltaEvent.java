package com.travelagent.travelplanner.chat.api;

/**
 * SSE 增量事件。
 *
 * @param messageId 助手消息主键
 * @param sequence 从 1 开始递增的序号，可直接作为 SSE id
 * @param content 增量文本
 */
public record ChatDeltaEvent(long messageId, long sequence, String content) {
}
