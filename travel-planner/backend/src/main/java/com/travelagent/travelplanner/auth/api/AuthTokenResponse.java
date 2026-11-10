package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.auth.application.AuthToken;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录令牌响应。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 * @param tokenType Bearer
 * @param expiresIn access token 有效秒数
 */
public record AuthTokenResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String refreshToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String tokenType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long expiresIn) {

    /**
     * 从应用层结果转换。
     *
     * @param token 应用层令牌
     * @return API 响应
     */
    public static AuthTokenResponse from(AuthToken token) {
        return new AuthTokenResponse(token.accessToken(), token.refreshToken(), token.tokenType(), token.expiresIn());
    }
}
