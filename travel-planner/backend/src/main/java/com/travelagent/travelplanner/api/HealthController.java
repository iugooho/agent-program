package com.travelagent.travelplanner.api;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * @return 统一响应结构，data 里带服务名、状态与服务器时间
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "travel-planner-backend");
        data.put("status", "UP");
        data.put("time", OffsetDateTime.now().toString());
        return ApiResponse.ok(data);
    }
}
