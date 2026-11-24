package com.travelagent.travelplanner.auth.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** auth_session 表 Mapper。 */
@Mapper
public interface AuthSessionMapper {

    /**
     * 创建登录会话。
     *
     * @param sessionId 会话标识
     * @param userId 用户主键
     * @param clientIp 客户端 IP
     * @param userAgent 客户端标识
     * @param expiresAt 过期时间
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO auth_session (
                session_id, user_id, client_ip, user_agent, expires_at
            ) VALUES (
                #{sessionId}, #{userId}, #{clientIp}, #{userAgent}, #{expiresAt}
            )
            """)
    int insert(
            @Param("sessionId") String sessionId,
            @Param("userId") long userId,
            @Param("clientIp") String clientIp,
            @Param("userAgent") String userAgent,
            @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * 更新会话活跃时间。
     *
     * @param sessionId 会话标识
     * @param expiresAt 新过期时间
     * @return 更新行数
     */
    @Update("""
            UPDATE auth_session
            SET last_seen_time = CURRENT_TIMESTAMP(6),
                expires_at = #{expiresAt}
            WHERE session_id = #{sessionId}
              AND revoked_at IS NULL
            """)
    int touch(
            @Param("sessionId") String sessionId,
            @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * 查询用户有效会话。
     *
     * @param userId 用户主键
     * @return 会话行
     */
    @Select("""
            SELECT session_id AS sessionId, user_id AS userId,
                   client_ip AS clientIp, user_agent AS userAgent,
                   create_time AS createTime, last_seen_time AS lastSeenTime,
                   expires_at AS expiresAt, revoked_at AS revokedAt
            FROM auth_session
            WHERE user_id = #{userId}
              AND revoked_at IS NULL
              AND expires_at > CURRENT_TIMESTAMP(6)
            ORDER BY last_seen_time DESC
            """)
    List<AuthSessionRow> findActiveByUser(@Param("userId") long userId);

    /**
     * 撤销用户指定会话。
     *
     * @param userId 用户主键
     * @param sessionId 会话标识
     * @return 更新行数
     */
    @Update("""
            UPDATE auth_session
            SET revoked_at = CURRENT_TIMESTAMP(6)
            WHERE user_id = #{userId}
              AND session_id = #{sessionId}
              AND revoked_at IS NULL
            """)
    int revoke(
            @Param("userId") long userId,
            @Param("sessionId") String sessionId);

    /**
     * 撤销用户全部会话。
     *
     * @param userId 用户主键
     * @return 更新行数
     */
    @Update("""
            UPDATE auth_session
            SET revoked_at = CURRENT_TIMESTAMP(6)
            WHERE user_id = #{userId}
              AND revoked_at IS NULL
            """)
    int revokeAll(@Param("userId") long userId);
}
