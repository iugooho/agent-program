package com.travelagent.travelplanner.auth.domain;

import java.time.LocalDateTime;

/**
 * 通知偏好。
 *
 * @param userId 用户主键
 * @param tripReminder 行程提醒
 * @param tripChange 行程变更
 * @param systemMessage 系统消息
 * @param emailEnabled 邮件通知
 * @param updateTime 更新时间
 */
public record NotificationPreferences(
        long userId,
        boolean tripReminder,
        boolean tripChange,
        boolean systemMessage,
        boolean emailEnabled,
        LocalDateTime updateTime) {

    /** 默认通知偏好。 */
    public static NotificationPreferences defaults(long userId) {
        return new NotificationPreferences(userId, true, true, true, true, null);
    }
}
