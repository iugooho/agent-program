package com.travelagent.travelplanner.auth.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * user_notification_preferences 表持久化行。
 *
 * @param userId 用户主键
 * @param tripReminder 行程提醒
 * @param tripChange 行程变更
 * @param systemMessage 系统消息
 * @param emailEnabled 邮件通知
 * @param updateTime 更新时间
 */
public record NotificationPreferencesRow(
        long userId,
        boolean tripReminder,
        boolean tripChange,
        boolean systemMessage,
        boolean emailEnabled,
        LocalDateTime updateTime) {
}
