package com.travelagent.travelplanner.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 统一分页响应。
 *
 * @param page 当前页码，从 1 开始
 * @param size 每页条数
 * @param total 总条数
 * @param items 当前页数据
 * @param <T> 数据项类型
 */
public record PageResult<T>(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long total,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> items) {

    /**
     * 创建分页响应。
     *
     * @param page 当前页码
     * @param size 每页条数
     * @param total 总条数
     * @param items 当前页数据
     */
    public PageResult {
        if (page < 1) {
            throw new IllegalArgumentException("page 必须从 1 开始");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size 必须大于 0");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total 不能小于 0");
        }
        items = List.copyOf(items);
    }
}
