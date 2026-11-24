package com.travelagent.travelplanner.chat.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * conversations 表查询行。
 *
 * @param id 会话主键
 * @param userId 所属用户
 * @param title 标题
 * @param status 状态
 * @param lastMessageTime 最近消息时间
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record ConversationRow(
        long id,
        long userId,
        String title,
        String status,
        LocalDateTime lastMessageTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
