package com.travelagent.travelplanner.auth.domain;

import java.time.Duration;

/** 登录会话撤销存储，生产实现使用 Redis。 */
public interface SessionRevocationStore {

    /**
     * 撤销会话。
     *
     * @param sessionId 会话标识
     * @param ttl 至少覆盖 Refresh Token 剩余有效期
     */
    void revoke(String sessionId, Duration ttl);

    /**
     * 判断会话是否已撤销。
     *
     * @param sessionId 会话标识
     * @return true 表示已撤销
     */
    boolean isRevoked(String sessionId);
}
