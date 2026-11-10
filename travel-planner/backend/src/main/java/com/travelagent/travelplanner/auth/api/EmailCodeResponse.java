package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.auth.application.EmailCodeDelivery;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 邮箱验证码发送响应。
 *
 * @param expiresIn 有效期秒数
 * @param retryAfterSeconds 冷却秒数
 */
public record EmailCodeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long expiresIn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long retryAfterSeconds) {

    /**
     * 从应用层结果转换。
     *
     * @param delivery 应用层结果
     * @return API 响应
     */
    public static EmailCodeResponse from(EmailCodeDelivery delivery) {
        return new EmailCodeResponse(delivery.expiresIn(), delivery.retryAfterSeconds());
    }
}
