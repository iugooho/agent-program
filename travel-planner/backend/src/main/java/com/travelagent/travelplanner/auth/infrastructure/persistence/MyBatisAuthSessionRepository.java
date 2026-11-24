package com.travelagent.travelplanner.auth.infrastructure.persistence;

import com.travelagent.travelplanner.auth.domain.AuthSession;
import com.travelagent.travelplanner.auth.domain.AuthSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的登录会话仓储。 */
@Repository
public class MyBatisAuthSessionRepository implements AuthSessionRepository {

    private final AuthSessionMapper mapper;

    /**
     * 创建会话仓储。
     *
     * @param mapper 会话 Mapper
     */
    public MyBatisAuthSessionRepository(AuthSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void create(AuthSession session) {
        mapper.insert(
                session.sessionId(),
                session.userId(),
                session.clientIp(),
                session.userAgent(),
                session.expiresAt());
    }

    @Override
    public int touch(String sessionId, LocalDateTime expiresAt) {
        return mapper.touch(sessionId, expiresAt);
    }

    @Override
    public List<AuthSession> findActiveByUser(long userId) {
        return mapper.findActiveByUser(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int revoke(long userId, String sessionId) {
        return mapper.revoke(userId, sessionId);
    }

    @Override
    public int revokeAll(long userId) {
        return mapper.revokeAll(userId);
    }

    private AuthSession toDomain(AuthSessionRow row) {
        return new AuthSession(
                row.sessionId(),
                row.userId(),
                row.clientIp(),
                row.userAgent(),
                row.createTime(),
                row.lastSeenTime(),
                row.expiresAt(),
                row.revokedAt());
    }
}
