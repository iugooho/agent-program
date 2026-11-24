package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.itinerary.api.TravelPlanSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 当前用户个人中心概览。
 *
 * @param totalTrips 累计行程数
 * @param upcomingTrips 即将出发行程数
 * @param cities 去过的城市数
 * @param totalDays 累计旅行天数
 * @param nextTrip 下一段行程
 * @param recentTrips 最近行程
 */
public record UserOverviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalTrips,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int upcomingTrips,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int cities,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalDays,
        TravelPlanSummaryResponse nextTrip,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<TravelPlanSummaryResponse> recentTrips) {
}
