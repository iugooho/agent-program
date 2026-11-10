package com.travelagent.travelplanner.auth.infrastructure.security;

import com.travelagent.travelplanner.auth.application.AccessTokenClaims;
import com.travelagent.travelplanner.auth.application.TokenService;
import com.travelagent.travelplanner.auth.domain.SessionRevocationStore;
import com.travelagent.travelplanner.common.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 从 Authorization: Bearer 请求头恢复登录态。 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final SessionRevocationStore sessionRevocationStore;

    /**
     * 创建 JWT 过滤器。
     *
     * @param tokenService 令牌服务
     * @param sessionRevocationStore 会话撤销存储
     */
    public JwtAuthenticationFilter(TokenService tokenService, SessionRevocationStore sessionRevocationStore) {
        this.tokenService = tokenService;
        this.sessionRevocationStore = sessionRevocationStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null
                && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            authenticate(authorization.substring(BEARER_PREFIX.length()).strip());
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            AccessTokenClaims claims = tokenService.validateAccessToken(token);
            if (sessionRevocationStore.isRevoked(claims.sessionId())) {
                return;
            }
            AuthenticatedUser principal = new AuthenticatedUser(
                    claims.userId(), claims.username(), claims.role(), claims.sessionId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
        }
    }
}
