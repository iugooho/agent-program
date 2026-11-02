package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.DayPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** 单日行程响应。 */
public record DayPlanResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int dayNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String city,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ItineraryItemResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> notes) {

    /** 从领域模型转换。 */
    public static DayPlanResponse from(DayPlan day) {
        return new DayPlanResponse(day.dayNumber(), day.date(), day.city(),
                day.items().stream().map(ItineraryItemResponse::from).toList(), day.notes());
    }
}
