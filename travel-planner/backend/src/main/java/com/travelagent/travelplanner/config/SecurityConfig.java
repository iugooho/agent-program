package com.travelagent.travelplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置骨架：健康检查与接口文档放行，其余接口需要认证。
 *
 * <p>登录签发 JWT、按角色做接口授权（@PreAuthorize）等业务规则请在业务代码里补齐；
 * 生产环境还要把 CORS、CSRF、限流策略一起定下来。</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * HTTP 安全规则。
     *
     * @param http Spring Security 的 HTTP 配置入口
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/health", "/api/v1/demo/hello", "/actuator/health",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
