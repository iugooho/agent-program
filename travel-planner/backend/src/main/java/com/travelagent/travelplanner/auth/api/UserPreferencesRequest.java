package com.travelagent.travelplanner.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 更新用户长期偏好请求。
 *
 * @param travelStyleDefault 默认出行风格
 * @param foodHabit 饮食偏好
 * @param forbiddenList 禁忌清单
 * @param accommodationPreference 住宿偏好
 * @param budgetLevel 预算等级
 */
public record UserPreferencesRequest(
        @Size(max = 50) String travelStyleDefault,
        @Size(max = 200) String foodHabit,
        @Size(max = 50) List<@Size(max = 50) String> forbiddenList,
        @Size(max = 200) String accommodationPreference,
        @Pattern(regexp = "^$|ECONOMY|COMFORT|LUXURY")
        @Schema(example = "COMFORT") String budgetLevel) {
}
