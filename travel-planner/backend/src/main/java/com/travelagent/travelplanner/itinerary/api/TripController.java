package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.itinerary.application.TravelAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** 创建接口。 */
    public TripController(TravelAgentService travelAgentService) {
        this.travelAgentService = travelAgentService;
    }

    /** 同步生成一份旅行计划。 */
    @PostMapping("/plan")
    @PreAuthorize("hasAuthority('trip.create')")
    @Operation(summary = "生成旅行计划", description = "模型解析需求，工具补充实时事实，Java 校验预算和时间")
    public ApiResponse<TripPlanResponse> plan(@Valid @RequestBody TripPlanRequest request) {
        return ApiResponse.ok(TripPlanResponse.from(travelAgentService.plan(request.message())));
    }
}
