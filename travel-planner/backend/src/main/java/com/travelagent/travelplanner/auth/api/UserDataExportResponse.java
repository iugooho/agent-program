package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.itinerary.api.SavedTravelPlanResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 当前用户数据导出。
 *
 * @param profile 账号资料
 * @param preferences 旅行偏好
 * @param notificationPreferences 通知偏好
 * @param trips 本人旅行计划
 */
public record UserDataExportResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserResponse profile,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UserPreferencesResponse preferences,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        NotificationPreferencesResponse notificationPreferences,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<SavedTravelPlanResponse> trips) {
}
