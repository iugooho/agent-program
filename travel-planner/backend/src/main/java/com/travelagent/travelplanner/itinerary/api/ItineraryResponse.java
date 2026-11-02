package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** 智能体生成的完整行程。 */
public record ItineraryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalBudget,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BudgetSummaryResponse budgetSummary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DayPlanResponse> days,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> warnings,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<EvidenceResponse> evidences) {

    /** 从领域模型转换。 */
    public static ItineraryResponse from(Itinerary itinerary) {
        return new ItineraryResponse(itinerary.title(), itinerary.totalBudget(),
                BudgetSummaryResponse.from(itinerary.budgetSummary()),
                itinerary.days().stream().map(DayPlanResponse::from).toList(),
                itinerary.warnings(),
                itinerary.evidences().stream().map(EvidenceResponse::from).toList());
    }
}
