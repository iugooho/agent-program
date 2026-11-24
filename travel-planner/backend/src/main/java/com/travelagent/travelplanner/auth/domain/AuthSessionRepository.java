package com.travelagent.travelplanner.auth.domain;

import java.time.LocalDateTime;
import java.util.List;

/** 登录会话仓储。 */
public interface AuthSessionRepository {

    /**
     * 创建会话。
     *
     * @param session 会话
     */
    void create(AuthSession session);

    /**
     * 更新会话活跃时间与过期时间。
     *
     * @param sessionId 会话标识
     * @param expiresAt 新过期时间
     * @return 更新行数
     */
    int touch(String sessionId, LocalDateTime expiresAt);

    /**
     * 查询用户当前有效会话。
     *
     * @param userId 用户主键
     * @return 有效会话
     */
    List<AuthSession> findActiveByUser(long userId);

    /**
     * 撤销指定用户的会话。
     *
     * @param userId 用户主键
     * @param sessionId 会话标识
     * @return 更新行数
     */
    int revoke(long userId, String sessionId);

    /**
     * 撤销用户全部会话。
     *
     * @param userId 用户主键
     * @return 更新行数
     */
    int revokeAll(long userId);
}
