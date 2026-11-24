package com.travelagent.travelplanner.chat.domain;

import java.time.LocalDateTime;

/**
 * 一次旅行规划对话。
 *
 * @param id 会话主键
 * @param userId 所属用户
 * @param title 会话标题
 * @param status 会话状态
 * @param lastMessageTime 最近一条消息时间
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record Conversation(
        long id,
        long userId,
        String title,
        ConversationStatus status,
        LocalDateTime lastMessageTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}

