package com.travelagent.travelplanner.auth.api;

/**
 * 更新通知偏好。
 *
 * @param tripReminder 行程提醒
 * @param tripChange 行程变更
 * @param systemMessage 系统消息
 * @param emailEnabled 邮件通知
 */
public record NotificationPreferencesRequest(
        boolean tripReminder,
        boolean tripChange,
        boolean systemMessage,
        boolean emailEnabled) {
}
