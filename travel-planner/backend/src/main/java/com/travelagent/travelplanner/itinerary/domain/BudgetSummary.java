package com.travelagent.travelplanner.itinerary.domain;

import java.math.BigDecimal;

/** 行程预算拆分，金额均为整趟行程的估算值。 */
public record BudgetSummary(
        BigDecimal attractions,
        BigDecimal food,
        BigDecimal accommodationAndTransport,
        BigDecimal total) {
}
