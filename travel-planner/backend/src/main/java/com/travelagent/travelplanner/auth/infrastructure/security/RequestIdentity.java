package com.travelagent.travelplanner.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 从请求中提取稳定但脱敏的客户端标识。 */
public final class RequestIdentity {

    private RequestIdentity() {
    }

    /**
     * 获取客户端 IP。
     *
     * @param request HTTP 请求
     * @param trustForwardedHeader 是否信任代理头
     * @return IP
     */
    public static String clientIp(HttpServletRequest request, boolean trustForwardedHeader) {
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].strip();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 生成 IP 与 User-Agent 绑定键。
     *
     * @param request HTTP 请求
     * @param trustForwardedHeader 是否信任代理头
     * @return 哈希后的客户端键
     */
    public static String clientKey(HttpServletRequest request, boolean trustForwardedHeader) {
        String ip = clientIp(request, trustForwardedHeader);
        String userAgent = request.getHeader("User-Agent");
        return hash(ip + "|" + (userAgent == null ? "" : userAgent));
    }

    /**
     * 对客户端标识做不可逆哈希，避免在缓存键中保存原始 IP。
     *
     * @param value 原始值
     * @return SHA-256
     */
    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
