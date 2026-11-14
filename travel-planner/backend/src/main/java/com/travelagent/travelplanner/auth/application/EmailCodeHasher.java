package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 邮箱验证码摘要工具，验证码本身不写入数据库。 */
public final class EmailCodeHasher {

    private EmailCodeHasher() {
    }

    /**
     * 按用途和邮箱计算验证码摘要。
     *
     * @param email 邮箱
     * @param purpose 验证码用途
     * @param code 明文验证码
     * @return SHA-256 摘要
     */
    public static String hash(String email, EmailPurpose purpose, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = (purpose.name() + ":" + email + ":" + code)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
