package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.BudgetSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 行程预算拆分。 */
public record BudgetSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal attractions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal food,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal accommodationAndTransport,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal total) {

    /** 从领域模型转换。 */
    public static BudgetSummaryResponse from(BudgetSummary summary) {
        return new BudgetSummaryResponse(summary.attractions(), summary.food(),
                summary.accommodationAndTransport(), summary.total());
    }
}
