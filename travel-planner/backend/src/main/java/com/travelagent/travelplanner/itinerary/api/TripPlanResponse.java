package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.application.TravelAgentService.PlanningResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 一次智能体规划的完整结果。
 *
 * @param request 旅行需求
 * @param itinerary 行程方案
 * @param mode 生成模式
 * @param savedPlanId 保存后的计划主键
 */
public record TripPlanResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TripRequestResponse request,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ItineraryResponse itinerary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "deepseek+java") String mode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long savedPlanId) {

    /** 从应用层结果转换。 */
    public static TripPlanResponse from(PlanningResult result, long savedPlanId) {
        return new TripPlanResponse(TripRequestResponse.from(result.request()),
                ItineraryResponse.from(result.itinerary()), result.mode(), savedPlanId);
    }
}
