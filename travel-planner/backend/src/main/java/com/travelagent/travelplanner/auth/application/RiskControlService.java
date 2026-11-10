package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.CounterStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** 登录失败次数和 CAPTCHA 触发策略。 */
@Service
public class RiskControlService {

    private static final String PREFIX = "risk:login-fail:";

    private final CounterStore counterStore;
    private final AuthProperties.Captcha properties;

    public RiskControlService(CounterStore counterStore, AuthProperties authProperties) {
        this.counterStore = counterStore;
        this.properties = authProperties.captcha();
    }

    /**
     * 指定账号和客户端是否已经触发登录 CAPTCHA。
     *
     * @param username 登录账号
     * @param clientKey 客户端绑定信息
     * @return 是否需要 CAPTCHA
     */
    public boolean requiresCaptcha(String username, String clientKey) {
        return currentFailures(username, clientKey) >= properties.maxLoginFailures();
    }

    /**
     * 记录一次登录失败。
     *
     * @param username 登录账号
     * @param clientKey 客户端绑定信息
     * @return 当前失败次数
     */
    public long recordFailure(String username, String clientKey) {
        return counterStore.increment(key(username, clientKey), properties.failureWindow());
    }

    /**
     * 登录成功后清除失败次数。
     *
     * @param username 登录账号
     * @param clientKey 客户端绑定信息
     */
    public void clearFailures(String username, String clientKey) {
        counterStore.delete(key(username, clientKey));
    }

    private long currentFailures(String username, String clientKey) {
        return counterStore.get(key(username, clientKey));
    }

    private String key(String username, String clientKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String normalized = (username == null ? "" : username.strip().toLowerCase(Locale.ROOT))
                    + "|"
                    + clientKey;
            return PREFIX + HexFormat.of().formatHex(
                    digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
