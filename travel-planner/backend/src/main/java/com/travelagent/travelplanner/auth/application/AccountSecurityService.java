package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.api.SecurityEventResponse;
import com.travelagent.travelplanner.auth.api.UserSessionResponse;
import com.travelagent.travelplanner.auth.domain.AuthSession;
import com.travelagent.travelplanner.auth.domain.AuthSessionRepository;
import com.travelagent.travelplanner.auth.domain.SecurityEventRepository;
import com.travelagent.travelplanner.auth.domain.SecurityEventType;
import com.travelagent.travelplanner.auth.domain.SessionRevocationStore;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 登录会话和账号安全事件用例。 */
@Service
public class AccountSecurityService {

    private static final int SECURITY_EVENT_LIMIT = 20;

    private final AuthSessionRepository sessionRepository;
    private final SecurityEventRepository eventRepository;
    private final SessionRevocationStore revocationStore;
    private final TokenService tokenService;

    /**
     * 创建账号安全服务。
     *
     * @param sessionRepository 会话仓储
     * @param eventRepository 安全事件仓储
     * @param revocationStore 会话撤销存储
     * @param tokenService 令牌服务
     */
    public AccountSecurityService(
            AuthSessionRepository sessionRepository,
            SecurityEventRepository eventRepository,
            SessionRevocationStore revocationStore,
            TokenService tokenService) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.revocationStore = revocationStore;
        this.tokenService = tokenService;
    }

    /**
     * 登记新登录会话并写入登录事件。
     *
     * @param token 新令牌
     * @param context 客户端信息
     */
    @Transactional
    public void openSession(AuthToken token, ClientContext context) {
        sessionRepository.create(new AuthSession(
                token.sessionId(),
                token.userId(),
                context.clientIp(),
                context.userAgent(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.ofInstant(token.refreshExpiresAt(), ZoneId.systemDefault()),
                null));
        recordEvent(
                token.userId(),
                SecurityEventType.LOGIN,
                "账号登录成功",
                context);
    }

    /**
     * 刷新后更新会话活跃时间。
     *
     * @param token 新令牌
     * @param context 客户端信息
     */
    public void touchSession(AuthToken token, ClientContext context) {
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                token.refreshExpiresAt(),
                ZoneId.systemDefault());
        if (sessionRepository.touch(token.sessionId(), expiresAt) == 0) {
            sessionRepository.create(new AuthSession(
                    token.sessionId(),
                    token.userId(),
                    context.clientIp(),
                    context.userAgent(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    expiresAt,
                    null));
        }
    }

    /**
     * 查询当前用户有效会话。
     *
     * @param userId 用户主键
     * @param currentSessionId 当前会话标识
     * @return 会话列表
     */
    @Transactional(readOnly = true)
    public List<UserSessionResponse> sessions(long userId, String currentSessionId) {
        return sessionRepository.findActiveByUser(userId).stream()
                .map(session -> UserSessionResponse.from(session, currentSessionId))
                .toList();
    }

    /**
     * 查询当前用户最近安全事件。
     *
     * @param userId 用户主键
     * @return 安全事件
     */
    @Transactional(readOnly = true)
    public List<SecurityEventResponse> securityEvents(long userId) {
        return eventRepository.findRecent(userId, SECURITY_EVENT_LIMIT).stream()
                .map(SecurityEventResponse::from)
                .toList();
    }

    /**
     * 撤销指定会话。
     *
     * @param userId 用户主键
     * @param sessionId 目标会话
     * @param context 客户端信息
     */
    @Transactional
    public void revokeSession(long userId, String sessionId, ClientContext context) {
        if (sessionRepository.revoke(userId, sessionId) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "登录会话不存在");
        }
        revocationStore.revoke(sessionId, tokenService.refreshTokenLifetime());
        recordEvent(
                userId,
                SecurityEventType.SESSION_REVOKED,
                "已撤销一个登录会话",
                context);
    }

    /**
     * 退出当前会话。
     *
     * @param userId 用户主键
     * @param sessionId 当前会话
     * @param context 客户端信息
     */
    @Transactional
    public void logout(long userId, String sessionId, ClientContext context) {
        sessionRepository.revoke(userId, sessionId);
        revocationStore.revoke(sessionId, tokenService.refreshTokenLifetime());
        recordEvent(userId, SecurityEventType.LOGOUT, "账号退出登录", context);
    }

    /**
     * 撤销用户全部会话。
     *
     * @param userId 用户主键
     */
    public void revokeAllSessions(long userId) {
        List<AuthSession> activeSessions = sessionRepository.findActiveByUser(userId);
        activeSessions.forEach(session ->
                revocationStore.revoke(session.sessionId(), tokenService.refreshTokenLifetime()));
        sessionRepository.revokeAll(userId);
    }

    /**
     * 写入安全事件。
     *
     * @param userId 用户主键
     * @param type 事件类型
     * @param description 描述
     * @param context 客户端信息
     */
    public void recordEvent(
            long userId,
            SecurityEventType type,
            String description,
            ClientContext context) {
        eventRepository.create(
                userId,
                type.name(),
                description,
                context.clientIp(),
                context.userAgent());
    }
}
