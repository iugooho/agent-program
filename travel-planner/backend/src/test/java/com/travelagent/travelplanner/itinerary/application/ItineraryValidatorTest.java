package com.travelagent.travelplanner.itinerary.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.itinerary.domain.BudgetSummary;
import com.travelagent.travelplanner.itinerary.domain.DayPlan;
import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import com.travelagent.travelplanner.itinerary.domain.ItineraryItem;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 验证确定性的预算与时间校验。 */
class ItineraryValidatorTest {

    private final ItineraryValidator validator = new ItineraryValidator();

    @Test
    @DisplayName("超过预算的行程会被拒绝")
    void rejectsOverBudgetItinerary() {
        TripRequest request = request(BigDecimal.valueOf(1000L));
        Itinerary itinerary = itinerary(BigDecimal.valueOf(1200L));

        assertThatThrownBy(() -> validator.validateItinerary(request, itinerary))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过预算");
    }

    @Test
    @DisplayName("同一天时间重叠会被拒绝")
    void rejectsOverlappingItems() {
        TripRequest request = request(BigDecimal.valueOf(5000L));
        ItineraryItem first = item("景点 A", LocalTime.of(9, 0), LocalTime.of(11, 0));
        ItineraryItem second = item("景点 B", LocalTime.of(10, 30), LocalTime.of(12, 0));
        Itinerary itinerary = new Itinerary("测试", BigDecimal.valueOf(100L),
                new BudgetSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100L)),
                List.of(new DayPlan(1, LocalDate.now(), "杭州", List.of(first, second), List.of())),
                List.of(), List.of());

        assertThatThrownBy(() -> validator.validateItinerary(request, itinerary))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("时间冲突");
    }

    private TripRequest request(BigDecimal budget) {
        return new TripRequest("北京", List.of("杭州"), LocalDate.now(), 1, 1, budget,
                List.of("休闲"), "高铁", List.of(), List.of(), List.of());
    }

    private Itinerary itinerary(BigDecimal total) {
        ItineraryItem item = item("景点", LocalTime.of(9, 0), LocalTime.of(11, 0));
        return new Itinerary("测试", total,
                new BudgetSummary(total, BigDecimal.ZERO, BigDecimal.ZERO, total),
                List.of(new DayPlan(1, LocalDate.now(), "杭州", List.of(item), List.of())),
                List.of(), List.of());
    }

    private ItineraryItem item(String title, LocalTime start, LocalTime end) {
        return new ItineraryItem(title, "景点", start, end, "杭州", BigDecimal.valueOf(100L), "");
    }
}
