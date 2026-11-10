package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.domain.UserAccount;
import java.time.Duration;

/** JWT 签发和校验接口。 */
public interface TokenService {

    /**
     * 为用户创建新的令牌对。
     *
     * @param user 用户
     * @return 令牌对
     */
    AuthToken issue(UserAccount user);

    /**
     * 校验 Access Token。
     *
     * @param token JWT
     * @return 声明
     */
    AccessTokenClaims validateAccessToken(String token);

    /**
     * 校验 Refresh Token。
     *
     * @param token JWT
     * @return 声明
     */
    RefreshTokenClaims validateRefreshToken(String token);

    /**
     * Refresh Token 有效期，用于会话撤销缓存 TTL。
     *
     * @return 有效期
     */
    Duration refreshTokenLifetime();
}
