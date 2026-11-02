package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 智能体识别出的结构化旅行需求。 */
public record TripRequestResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String departureCity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> destinations,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int days,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int travelers,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal budget,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> travelStyles,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String transportPreference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> foodPreferences,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> forbiddenItems,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> accommodationPreferences) {

    /** 从领域模型转换。 */
    public static TripRequestResponse from(TripRequest request) {
        return new TripRequestResponse(request.departureCity(), request.destinations(), request.startDate(),
                request.days(), request.travelers(), request.budget(), request.travelStyles(),
                request.transportPreference(), request.foodPreferences(), request.forbiddenItems(),
                request.accommodationPreferences());
    }
}
