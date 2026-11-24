package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.api.UserOverviewResponse;
import com.travelagent.travelplanner.itinerary.api.TravelPlanSummaryResponse;
import com.travelagent.travelplanner.itinerary.application.TravelPlanService;
import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 个人中心聚合数据用例。 */
@Service
public class UserOverviewService {

    private static final int RECENT_TRIP_LIMIT = 5;

    private final TravelPlanService travelPlanService;

    /**
     * 创建个人中心概览服务。
     *
     * @param travelPlanService 旅行计划服务
     */
    public UserOverviewService(TravelPlanService travelPlanService) {
        this.travelPlanService = travelPlanService;
    }

    /**
     * 汇总当前用户本人创建的行程。
     *
     * @param userId 当前用户主键
     * @return 个人中心概览
     */
    @Transactional(readOnly = true)
    public UserOverviewResponse overview(long userId) {
        LocalDate today = LocalDate.now();
        List<SavedTravelPlan> plans = travelPlanService.findOwned(userId);
        List<SavedTravelPlan> upcoming = plans.stream()
                .filter(plan -> !plan.request().startDate().isBefore(today))
                .sorted(Comparator.comparing(plan -> plan.request().startDate()))
                .toList();
        SavedTravelPlan nextTrip = upcoming.isEmpty() ? null : upcoming.get(0);
        List<TravelPlanSummaryResponse> recentTrips = plans.stream()
                .sorted(Comparator.comparing(SavedTravelPlan::createTime).reversed())
                .limit(RECENT_TRIP_LIMIT)
                .map(TravelPlanSummaryResponse::from)
                .toList();
        int cities = (int) plans.stream()
                .flatMap(plan -> plan.request().destinations().stream())
                .distinct()
                .count();
        long totalDays = plans.stream()
                .mapToLong(plan -> plan.request().days())
                .sum();
        return new UserOverviewResponse(
                plans.size(),
                upcoming.size(),
                cities,
                totalDays,
                nextTrip == null ? null : TravelPlanSummaryResponse.from(nextTrip),
                recentTrips);
    }
}
