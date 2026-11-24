package com.travelagent.travelplanner.chat.api;

import com.travelagent.travelplanner.chat.domain.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 对话消息响应。
 *
 * @param id 消息主键
 * @param conversationId 所属会话
 * @param role USER、ASSISTANT 或 SYSTEM
 * @param content 消息内容
 * @param msgType TEXT 或 STREAM
 * @param status PENDING、STREAMING、COMPLETED 或 FAILED
 * @param model 生成模型标识
 * @param errorMessage 失败原因
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record ChatMessageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long conversationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String role,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String msgType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        String model,
        String errorMessage,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updateTime) {

    /**
     * 从领域对象转换。
     *
     * @param message 消息
     * @return 响应
     */
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.id(),
                message.conversationId(),
                message.role().name(),
                message.content(),
                message.type().name(),
                message.status().name(),
                message.model(),
                message.errorMessage(),
                message.createTime(),
                message.updateTime());
    }
}
