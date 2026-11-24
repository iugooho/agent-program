package com.travelagent.travelplanner.chat.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * 会话列表查询行。
 *
 * @param id 会话主键
 * @param title 标题
 * @param status 状态
 * @param messageCount 消息条数
 * @param lastMessagePreview 最近一条消息原文
 * @param lastMessageTime 最近消息时间
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record ConversationSummaryRow(
        long id,
        String title,
        String status,
        int messageCount,
        String lastMessagePreview,
        LocalDateTime lastMessageTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
