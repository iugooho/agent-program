package com.travelagent.travelplanner.auth.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * user_preferences 查询行。
 *
 * @param userId 用户主键
 * @param travelStyleDefault 默认风格
 * @param foodHabit 饮食偏好
 * @param forbiddenList 禁忌 JSON
 * @param accommodationPreference 住宿偏好
 * @param budgetLevel 预算等级
 * @param updateTime 更新时间
 */
public record UserPreferencesRow(
        long userId,
        String travelStyleDefault,
        String foodHabit,
        String forbiddenList,
        String accommodationPreference,
        String budgetLevel,
        LocalDateTime updateTime) {
}
