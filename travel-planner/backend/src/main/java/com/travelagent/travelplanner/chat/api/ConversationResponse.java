package com.travelagent.travelplanner.chat.api;

import com.travelagent.travelplanner.chat.domain.Conversation;
import com.travelagent.travelplanner.chat.domain.ConversationSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 会话响应。
 *
 * @param id 会话主键
 * @param title 标题
 * @param status 状态
 * @param messageCount 消息条数
 * @param lastMessagePreview 最近消息摘要
 * @param lastMessageTime 最近消息时间
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record ConversationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int messageCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String lastMessagePreview,
        LocalDateTime lastMessageTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updateTime) {

    /**
     * 从会话摘要转换。
     *
     * @param summary 摘要
     * @param previewMaxLength 摘要最大长度
     * @return 响应
     */
    public static ConversationResponse from(ConversationSummary summary, int previewMaxLength) {
        return new ConversationResponse(
                summary.id(),
                summary.title(),
                summary.status().name(),
                summary.messageCount(),
                preview(summary.lastMessagePreview(), previewMaxLength),
                summary.lastMessageTime(),
                summary.createTime(),
                summary.updateTime());
    }

    /**
     * 从会话实体转换。
     *
     * @param conversation 会话
     * @param messageCount 消息条数
     * @param lastMessage 最近一条消息原文
     * @param previewMaxLength 摘要最大长度
     * @return 响应
     */
    public static ConversationResponse from(
            Conversation conversation, int messageCount, String lastMessage, int previewMaxLength) {
        return new ConversationResponse(
                conversation.id(),
                conversation.title(),
                conversation.status().name(),
                messageCount,
                preview(lastMessage, previewMaxLength),
                conversation.lastMessageTime(),
                conversation.createTime(),
                conversation.updateTime());
    }

    private static String preview(String content, int maxLength) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").strip();
        int limit = Math.max(1, maxLength);
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "…";
    }
}
