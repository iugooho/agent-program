package com.travelagent.travelplanner.chat.domain;

import java.time.LocalDateTime;

/**
 * 会话列表投影：会话本身加上消息数和最近一条消息摘要。
 *
 * @param id 会话主键
 * @param title 会话标题
 * @param status 会话状态
 * @param messageCount 消息条数
 * @param lastMessagePreview 最近一条消息摘要
 * @param lastMessageTime 最近一条消息时间
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record ConversationSummary(
        long id,
        String title,
        ConversationStatus status,
        int messageCount,
        String lastMessagePreview,
        LocalDateTime lastMessageTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}

