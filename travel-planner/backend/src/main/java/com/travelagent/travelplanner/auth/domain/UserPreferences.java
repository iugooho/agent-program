package com.travelagent.travelplanner.auth.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户长期旅行偏好。
 *
 * @param userId 用户主键
 * @param travelStyleDefault 默认出行风格
 * @param foodHabit 饮食偏好
 * @param forbiddenList 禁忌清单
 * @param accommodationPreference 住宿偏好
 * @param budgetLevel 预算等级
 * @param updateTime 更新时间
 */
public record UserPreferences(
        long userId,
        String travelStyleDefault,
        String foodHabit,
        List<String> forbiddenList,
        String accommodationPreference,
        String budgetLevel,
        LocalDateTime updateTime) {

    /** 空偏好。 */
    public UserPreferences {
        forbiddenList = forbiddenList == null ? List.of() : List.copyOf(forbiddenList);
    }
}
