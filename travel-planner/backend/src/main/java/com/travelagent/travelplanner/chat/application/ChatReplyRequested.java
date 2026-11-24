package com.travelagent.travelplanner.chat.application;

/**
 * 请求生成一条助手回复。
 *
 * <p>在发送消息的事务提交后发布，保证模型读到的消息一定已经落库。</p>
 *
 * @param conversationId 会话主键
 * @param replyMessageId 助手消息主键
 */
public record ChatReplyRequested(long conversationId, long replyMessageId) {
}
