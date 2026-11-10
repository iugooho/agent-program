package com.travelagent.travelplanner.auth.application;

/**
 * 登录令牌结果。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 * @param tokenType 固定为 Bearer
 * @param expiresIn access token 有效秒数
 */
public record AuthToken(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
