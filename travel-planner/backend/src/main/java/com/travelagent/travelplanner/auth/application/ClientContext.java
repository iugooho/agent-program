package com.travelagent.travelplanner.auth.application;

/**
 * 发起请求的客户端信息。
 *
 * @param clientIp 客户端 IP
 * @param userAgent 客户端标识
 */
public record ClientContext(String clientIp, String userAgent) {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 255;

    /** 规范化长度，避免超出数据库字段。 */
    public ClientContext {
        clientIp = truncate(clientIp, MAX_IP_LENGTH);
        userAgent = truncate(userAgent, MAX_USER_AGENT_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
