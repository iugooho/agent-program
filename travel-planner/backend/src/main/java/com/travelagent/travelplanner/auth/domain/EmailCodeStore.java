package com.travelagent.travelplanner.auth.domain;

import java.time.Duration;

/** 邮箱验证码存储接口，生产实现使用 Redis。 */
public interface EmailCodeStore {

    /**
     * 占用发送冷却时间。
     *
     * @param email 邮箱
     * @param purpose 用途
     * @param retryAfter 冷却时长
     * @return true 表示允许发送
     */
    boolean tryReserveSend(String email, EmailPurpose purpose, Duration retryAfter);

    /**
     * 保存验证码哈希。
     *
     * @param email 邮箱
     * @param purpose 用途
     * @param codeHash 验证码哈希
     * @param ttl 有效期
     */
    void saveCode(String email, EmailPurpose purpose, String codeHash, Duration ttl);

    /**
     * 原子校验并删除验证码。
     *
     * @param email 邮箱
     * @param purpose 用途
     * @param codeHash 验证码哈希
     * @return true 表示验证码正确且已消费
     */
    boolean consumeCode(String email, EmailPurpose purpose, String codeHash);
}
