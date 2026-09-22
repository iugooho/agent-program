package com.travelagent.travelplanner.api;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试专用的探针 Controller：只负责触发各类异常，不参与业务，也不进生产代码。
 *
 * <p>写成独立文件而不是测试类里的嵌套类，是因为 Spring 的组件扫描基于 ASM 读元数据，
 * 不把嵌套类当作候选组件。</p>
 *
 * <p>挂在 {@code error-probe} profile 下：测试代码会被 @SpringBootTest 的组件扫描看到，
 * 不加 profile 的话这些 /probe/** 路径会混进 OpenAPI 契约里。</p>
 */
@Profile("error-probe")
@RestController
@RequestMapping("/probe")
class ErrorProbeController {

    /**
     * 抛业务异常。
     *
     * @return 不会返回，方法体直接抛出
     */
    @GetMapping("/business")
    public ApiResponse<Void> business() {
        throw new BusinessException(ErrorCode.ITINERARY_NOT_FOUND);
    }

    /**
     * 抛带自定义文案的业务异常。
     *
     * @return 不会返回，方法体直接抛出
     */
    @GetMapping("/business-custom")
    public ApiResponse<Void> businessWithCustomMessage() {
        throw new BusinessException(ErrorCode.ITINERARY_NOT_FOUND, "行程 t-1 不存在");
    }

    /**
     * 触发请求体校验失败。
     *
     * @param request 探针请求体
     * @return 校验通过时的成功响应
     */
    @PostMapping("/validation")
    public ApiResponse<Void> validation(@Valid @RequestBody ProbeRequest request) {
        return ApiResponse.ok(null);
    }

    /**
     * 抛未预期的异常。
     *
     * @return 不会返回，方法体直接抛出
     */
    @GetMapping("/boom")
    public ApiResponse<Void> boom() {
        throw new IllegalStateException("boom");
    }

    /**
     * 探针请求体。
     *
     * @param name 不能为空
     */
    record ProbeRequest(@NotBlank String name) {
    }
}
