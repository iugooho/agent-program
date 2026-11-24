package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUserProvider;
import com.travelagent.travelplanner.itinerary.application.TravelAgentService;
import com.travelagent.travelplanner.itinerary.application.TravelPlanService;
import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 旅行规划智能体接口。 */
@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "旅行规划", description = "将自然语言旅行需求转换为经过规则校验的行程")
public class TripController {

    private final TravelAgentService travelAgentService;
    private final TravelPlanService travelPlanService;
    private final AuthenticatedUserProvider currentUserProvider;

    /**
     * 创建接口。
     *
     * @param travelAgentService 旅行规划服务
     * @param travelPlanService 旅行计划服务
     * @param currentUserProvider 当前用户提供器
     */
    public TripController(
            TravelAgentService travelAgentService,
            TravelPlanService travelPlanService,
            AuthenticatedUserProvider currentUserProvider) {
        this.travelAgentService = travelAgentService;
        this.travelPlanService = travelPlanService;
        this.currentUserProvider = currentUserProvider;
    }

    /** 同步生成一份旅行计划。 */
    @PostMapping("/plan")
    @PreAuthorize("hasAuthority('trip.create')")
    @Operation(summary = "生成旅行计划", description = "模型解析需求，工具补充实时事实，Java 校验预算和时间")
    public ApiResponse<TripPlanResponse> plan(
            @Valid @RequestBody TripPlanRequest request) {
        var result = travelAgentService.plan(request.message());
        SavedTravelPlan saved = travelPlanService.save(currentUserProvider.require().id(), result);
        return ApiResponse.ok(TripPlanResponse.from(result, saved.id()));
    }

    /** 查询当前用户数据范围内可见的计划。 */
    @GetMapping
    @PreAuthorize("hasAuthority('trip.read')")
    @Operation(summary = "查询旅行计划列表")
    public ApiResponse<List<TravelPlanSummaryResponse>> list() {
        return ApiResponse.ok(travelPlanService.findVisible(currentUserProvider.require().id())
                .stream()
                .map(TravelPlanSummaryResponse::from)
                .toList());
    }

    /** 查询单条旅行计划。 */
    @GetMapping("/{planId}")
    @PreAuthorize("hasAuthority('trip.read')")
    @Operation(summary = "查询旅行计划详情")
    public ApiResponse<SavedTravelPlanResponse> detail(
            @PathVariable long planId) {
        return ApiResponse.ok(SavedTravelPlanResponse.from(
                travelPlanService.requireVisible(currentUserProvider.require().id(), planId)));
    }

    /** 删除旅行计划。 */
    @DeleteMapping("/{planId}")
    @PreAuthorize("hasAuthority('trip.delete')")
    @Operation(summary = "删除旅行计划")
    public ApiResponse<Void> delete(
            @PathVariable long planId) {
        travelPlanService.delete(currentUserProvider.require().id(), planId);
        return ApiResponse.ok(null);
    }
}
