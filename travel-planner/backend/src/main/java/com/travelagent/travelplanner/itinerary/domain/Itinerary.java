package com.travelagent.travelplanner.itinerary.domain;

import java.math.BigDecimal;
import java.util.List;

/** 经过规则校验的结构化旅行方案。 */
public record Itinerary(
        String title,
        BigDecimal totalBudget,
        BudgetSummary budgetSummary,
        List<DayPlan> days,
        List<String> warnings,
        List<ToolEvidence> evidences) {
}
