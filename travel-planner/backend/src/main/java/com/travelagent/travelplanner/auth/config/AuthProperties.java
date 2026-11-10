package com.travelagent.travelplanner.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证模块配置。
 *
 * @param jwt JWT 参数
 * @param emailCode 邮箱验证码参数
 * @param redis Redis 参数
 * @param email 邮件投递参数
 * @param captcha 人机验证参数
 * @param rateLimit 接口限流参数
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        Jwt jwt,
        EmailCode emailCode,
        Redis redis,
        Email email,
        Captcha captcha,
        RateLimit rateLimit) {

    /**
     * JWT 参数。
     *
     * @param secret HMAC 密钥
     * @param issuer 签发者
     * @param accessTtl Access Token 有效期
     * @param refreshTtl Refresh Token 有效期
     */
    public record Jwt(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {
    }

    /**
     * 邮箱验证码参数。
     *
     * @param ttl 验证码有效期
     * @param retryAfter 重复发送冷却时间
     */
    public record EmailCode(Duration ttl, Duration retryAfter) {
    }

    /**
     * Redis 参数。
     *
     * @param enabled 是否启用 Redis
     * @param host 主机
     * @param port 端口
     * @param password 密码
     * @param database 数据库编号
     * @param ssl 是否使用 TLS
     * @param timeout 连接超时
     */
    public record Redis(
            boolean enabled,
            String host,
            int port,
            String password,
            int database,
            boolean ssl,
            Duration timeout) {
    }

    /**
     * 邮件投递参数。
     *
     * @param mode local、smtp 或 disabled
     * @param host SMTP 主机
     * @param port SMTP 端口
     * @param username SMTP 用户名
     * @param password SMTP 授权码或密码
     * @param from 发件人地址
     * @param sslEnabled 是否启用 SMTPS
     * @param starttlsEnabled 是否启用 STARTTLS
     * @param connectTimeout 连接超时
     * @param readTimeout 读取超时
     * @param writeTimeout 写入超时
     */
    public record Email(
            String mode,
            String host,
            int port,
            String username,
            String password,
            String from,
            boolean sslEnabled,
            boolean starttlsEnabled,
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout) {
    }

    /**
     * 人机验证参数。
     *
     * @param challengeTtl 验证码挑战有效期
     * @param maxLoginFailures 触发登录验证码前允许的失败次数
     * @param failureWindow 失败计数窗口
     */
    public record Captcha(
            Duration challengeTtl,
            int maxLoginFailures,
            Duration failureWindow) {
    }

    /**
     * 接口限流参数。
     *
     * @param enabled 是否启用
     * @param trustForwardedHeader 是否信任反向代理传入的 X-Forwarded-For
     * @param generalPerMinute 普通接口每分钟限制
     * @param authPerMinute 认证接口每分钟限制
     * @param captchaPerMinute 人机验证接口每分钟限制
     */
    public record RateLimit(
            boolean enabled,
            boolean trustForwardedHeader,
            int generalPerMinute,
            int authPerMinute,
            int captchaPerMinute) {
    }
}
