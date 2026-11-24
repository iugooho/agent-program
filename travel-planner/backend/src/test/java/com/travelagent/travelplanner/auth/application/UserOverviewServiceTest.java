package com.travelagent.travelplanner.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.auth.api.UserOverviewResponse;
import com.travelagent.travelplanner.itinerary.application.TravelPlanService;
import com.travelagent.travelplanner.itinerary.domain.BudgetSummary;
import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 个人中心行程聚合规则测试。 */
class UserOverviewServiceTest {

    @Test
    @DisplayName("概览只统计本人行程并返回最近与下一段行程")
    void aggregatesOwnedTrips() {
        TravelPlanService planService = mock(TravelPlanService.class);
        when(planService.findOwned(7L)).thenReturn(List.of(
                plan(3, "杭州四日", List.of("杭州"), LocalDate.now().plusDays(7), 4,
                        LocalDateTime.of(2026, 11, 20, 10, 0)),
                plan(2, "成都周末", List.of("成都"), LocalDate.now().plusDays(2), 3,
                        LocalDateTime.of(2026, 11, 18, 10, 0)),
                plan(1, "厦门假期", List.of("厦门", "泉州"), LocalDate.now().minusDays(20), 5,
                        LocalDateTime.of(2026, 10, 1, 10, 0))));

        UserOverviewResponse overview = new UserOverviewService(planService).overview(7L);

        assertThat(overview.totalTrips()).isEqualTo(3);
        assertThat(overview.upcomingTrips()).isEqualTo(2);
        assertThat(overview.cities()).isEqualTo(4);
        assertThat(overview.totalDays()).isEqualTo(12);
        assertThat(overview.nextTrip().title()).isEqualTo("成都周末");
        assertThat(overview.recentTrips())
                .extracting("title")
                .containsExactly("杭州四日", "成都周末", "厦门假期");
    }

    private SavedTravelPlan plan(
            long id,
            String title,
            List<String> destinations,
            LocalDate startDate,
            int days,
            LocalDateTime createTime) {
        TripRequest request = new TripRequest(
                "上海",
                destinations,
                startDate,
                days,
                2,
                BigDecimal.valueOf(8000),
                List.of("RELAXED"),
                "高铁",
                List.of(),
                List.of(),
                List.of());
        BudgetSummary budget = new BudgetSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(8000));
        Itinerary itinerary = new Itinerary(
                title,
                BigDecimal.valueOf(8000),
                budget,
                List.of(),
                List.of(),
                List.of());
        return new SavedTravelPlan(id, 7L, 1L, request, itinerary, "AI", createTime);
    }
}
