package com.travelagent.travelplanner.auth.domain;

import java.time.Duration;

/** 人机验证挑战和一次性票据存储。 */
public interface CaptchaStore {

    /**
     * 保存验证码挑战。
     *
     * @param challengeId 挑战标识
     * @param clientKey 客户端绑定信息
     * @param purpose 用途
     * @param answerHash 答案哈希
     * @param ttl 有效期
     */
    void saveChallenge(
            String challengeId,
            String clientKey,
            CaptchaPurpose purpose,
            String answerHash,
            Duration ttl);

    /**
     * 原子消费验证码挑战。
     *
     * @param challengeId 挑战标识
     * @param clientKey 客户端绑定信息
     * @param purpose 用途
     * @param answerHash 答案哈希
     * @return 是否匹配并消费成功
     */
    boolean consumeChallenge(
            String challengeId,
            String clientKey,
            CaptchaPurpose purpose,
            String answerHash);
}
