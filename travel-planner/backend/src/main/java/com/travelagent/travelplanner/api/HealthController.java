package com.travelagent.travelplanner.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口：前端联调、容器健康检查都打这个接口，用来确认前后端是否打通。
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /**
     * 健康检查。
     *
     * @return 统一响应结构，data 为具名的 {@link HealthResponse}
     */
    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(HealthResponse.up());
    }
}
