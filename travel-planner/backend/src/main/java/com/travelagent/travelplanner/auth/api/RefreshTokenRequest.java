package com.travelagent.travelplanner.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求。
 *
 * @param refreshToken Refresh Token
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
