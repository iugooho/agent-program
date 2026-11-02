package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.application.TravelAgentService.PlanningResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 一次智能体规划的完整结果。 */
public record TripPlanResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TripRequestResponse request,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ItineraryResponse itinerary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "deepseek+java") String mode) {

    /** 从应用层结果转换。 */
    public static TripPlanResponse from(PlanningResult result) {
        return new TripPlanResponse(TripRequestResponse.from(result.request()),
                ItineraryResponse.from(result.itinerary()), result.mode());
    }
}
