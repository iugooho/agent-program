package com.travelagent.travelplanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.application.RateLimitService;
import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.infrastructure.security.RequestIdentity;
import com.travelagent.travelplanner.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 基于客户端 IP 的通用接口限流。 */
@Component
@ConditionalOnBean(RateLimitService.class)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectProvider<RateLimitService> rateLimitServiceProvider;
    private final AuthProperties.RateLimit properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            ObjectProvider<RateLimitService> rateLimitServiceProvider,
            AuthProperties authProperties,
            ObjectMapper objectMapper) {
        this.rateLimitServiceProvider = rateLimitServiceProvider;
        this.properties = authProperties.rateLimit();
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = properties.generalPerMinute();
        String bucket = "general";
        if (path.startsWith("/api/v1/auth/captcha/")) {
            limit = properties.captchaPerMinute();
            bucket = "captcha";
        } else if (path.startsWith("/api/v1/auth/")) {
            limit = properties.authPerMinute();
            bucket = "auth";
        }
        String ip = RequestIdentity.clientIp(request, properties.trustForwardedHeader());
        RateLimitService rateLimitService = rateLimitServiceProvider.getIfAvailable();
        if (rateLimitService == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!rateLimitService.isAllowed(
                "rate:" + bucket,
                RequestIdentity.hash(ip),
                limit,
                Duration.ofMinutes(1))) {
            writeRateLimited(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeRateLimited(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.RATE_LIMITED.httpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(ErrorCode.RATE_LIMITED.code(), ErrorCode.RATE_LIMITED.message()));
    }
}
