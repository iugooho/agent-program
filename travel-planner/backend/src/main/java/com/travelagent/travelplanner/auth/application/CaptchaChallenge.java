package com.travelagent.travelplanner.auth.application;

/**
 * 人机验证挑战。
 *
 * @param challengeId 挑战标识
 * @param imageDataUrl 验证码图片
 * @param expiresIn 有效秒数
 */
public record CaptchaChallenge(String challengeId, String imageDataUrl, long expiresIn) {
}
