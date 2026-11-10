package com.travelagent.travelplanner.auth.application;

/**
 * 邮箱验证码发送结果。
 *
 * @param expiresIn 有效期秒数
 * @param retryAfterSeconds 下次允许发送的冷却秒数
 */
public record EmailCodeDelivery(long expiresIn, long retryAfterSeconds) {
}
