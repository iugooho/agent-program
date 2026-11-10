package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.CaptchaGenerator;
import com.travelagent.travelplanner.auth.domain.CaptchaPurpose;
import com.travelagent.travelplanner.auth.domain.CaptchaStore;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** 人机验证挑战、答案和一次性票据用例。 */
@Service
public class CaptchaService {

    private final CaptchaGenerator generator;
    private final CaptchaStore store;
    private final AuthProperties.Captcha properties;

    public CaptchaService(
            CaptchaGenerator generator,
            CaptchaStore store,
            AuthProperties authProperties) {
        this.generator = generator;
        this.store = store;
        this.properties = authProperties.captcha();
    }

    /**
     * 创建人机验证挑战。
     *
     * @param purposeValue 用途
     * @param clientKey 客户端绑定信息
     * @return 挑战
     */
    public CaptchaChallenge create(String purposeValue, String clientKey) {
        CaptchaPurpose purpose = CaptchaPurpose.fromValue(purposeValue);
        String challengeId = UUID.randomUUID().toString();
        CaptchaGenerator.GeneratedCaptcha generated = generator.generate();
        store.saveChallenge(
                challengeId,
                clientKey,
                purpose,
                answerHash(challengeId, generated.answer()),
                properties.challengeTtl());
        return new CaptchaChallenge(
                challengeId,
                generated.imageDataUrl(),
                properties.challengeTtl().toSeconds());
    }

    /**
     * 在业务操作提交时校验并消费挑战。
     *
     * @param challengeId 挑战标识
     * @param answer 用户答案
     * @param purposeValue 用途
     * @param clientKey 客户端绑定信息
     */
    public void validate(
            String challengeId,
            String answer,
            String purposeValue,
            String clientKey) {
        if (challengeId == null || challengeId.isBlank() || answer == null || answer.isBlank()) {
            throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
        }
        CaptchaPurpose purpose = CaptchaPurpose.fromValue(purposeValue);
        boolean valid = store.consumeChallenge(
                challengeId,
                clientKey,
                purpose,
                answerHash(challengeId, answer.strip().toUpperCase(Locale.ROOT)));
        if (!valid) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
    }

    private String answerHash(String challengeId, String answer) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (challengeId + ":" + answer).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
