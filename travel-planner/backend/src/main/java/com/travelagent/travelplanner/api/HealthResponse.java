package com.travelagent.travelplanner.api;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 健康检查响应体，即 {@code ApiResponse.data} 的结构。
 *
 * <p>用具名 record 而不是 Map，是为了让 springdoc 能从类型上推导出 OpenAPI schema。
 * 返回 Map 时 schema 是空对象，前端生成不出类型，契约也就无从谈起。</p>
 *
 * <p>三个字段都标了 REQUIRED：record 序列化时键一定存在，标注后前端生成的类型才是非可选的。</p>
 *
 * @param service 服务名
 * @param status  服务状态
 * @param time    服务器时间，ISO-8601 格式
 */
public record HealthResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String service,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String time) {

    /**
     * 构造当前服务的健康检查响应体。
     *
     * @return 健康检查响应体
     */
    public static HealthResponse up() {
        return new HealthResponse("travel-planner-backend", "UP", OffsetDateTime.now().toString());
    }
}
