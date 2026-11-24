package com.travelagent.travelplanner.auth.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travelagent.travelplanner.auth.domain.NotificationPreferences;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 通知偏好响应。
 *
 * @param tripReminder 行程提醒
 * @param tripChange 行程变更
 * @param systemMessage 系统消息
 * @param emailEnabled 邮件通知
 * @param updateTime 更新时间
 */
public record NotificationPreferencesResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean tripReminder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean tripChange,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean systemMessage,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean emailEnabled,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updateTime) {

    /** 从领域对象转换。 */
    public static NotificationPreferencesResponse from(NotificationPreferences preferences) {
        return new NotificationPreferencesResponse(
                preferences.tripReminder(),
                preferences.tripChange(),
                preferences.systemMessage(),
                preferences.emailEnabled(),
                preferences.updateTime());
    }
}
