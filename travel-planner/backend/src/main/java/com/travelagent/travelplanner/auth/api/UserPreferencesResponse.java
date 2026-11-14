package com.travelagent.travelplanner.auth.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travelagent.travelplanner.auth.domain.UserPreferences;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户长期偏好响应。
 *
 * @param travelStyleDefault 默认出行风格
 * @param foodHabit 饮食偏好
 * @param forbiddenList 禁忌清单
 * @param accommodationPreference 住宿偏好
 * @param budgetLevel 预算等级
 * @param updateTime 更新时间
 */
public record UserPreferencesResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String travelStyleDefault,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String foodHabit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> forbiddenList,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accommodationPreference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String budgetLevel,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updateTime) {

    /**
     * 从领域偏好转换。
     *
     * @param preferences 领域偏好
     * @return API 响应
     */
    public static UserPreferencesResponse from(UserPreferences preferences) {
        return new UserPreferencesResponse(
                preferences.travelStyleDefault(),
                preferences.foodHabit(),
                preferences.forbiddenList(),
                preferences.accommodationPreference(),
                preferences.budgetLevel(),
                preferences.updateTime());
    }
}
