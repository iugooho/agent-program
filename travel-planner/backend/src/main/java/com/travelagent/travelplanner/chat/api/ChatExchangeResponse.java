package com.travelagent.travelplanner.chat.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 发送消息后的响应：用户消息已落库，助手消息以 PENDING 占位，
 * 前端凭 reply.id 订阅 SSE 获取增量。
 *
 * @param conversation 最新会话信息
 * @param userMessage 用户消息
 * @param reply 助手消息占位
 */
public record ChatExchangeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ConversationResponse conversation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ChatMessageResponse userMessage,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ChatMessageResponse reply) {
}
