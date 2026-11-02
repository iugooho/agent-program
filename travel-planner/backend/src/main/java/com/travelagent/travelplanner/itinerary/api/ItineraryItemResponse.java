package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.ItineraryItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalTime;

/** 行程中的单项安排。 */
public record ItineraryItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalTime startTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalTime endTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String location,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal estimatedCost,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String notes) {

    /** 从领域模型转换。 */
    public static ItineraryItemResponse from(ItineraryItem item) {
        return new ItineraryItemResponse(item.title(), item.type(), item.startTime(), item.endTime(),
                item.location(), item.estimatedCost(), item.notes());
    }
}
