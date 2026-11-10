package com.travelagent.travelplanner.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.CaptchaGenerator;
import com.travelagent.travelplanner.auth.infrastructure.cache.InMemoryCaptchaStore;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 人机验证挑战和一次性票据测试。 */
class CaptchaServiceTest {

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                null,
                null,
                null,
                null,
                new AuthProperties.Captcha(
                        Duration.ofMinutes(2),
                        3,
                        Duration.ofMinutes(10)),
                new AuthProperties.RateLimit(true, false, 120, 60, 20));
        CaptchaGenerator generator = () -> new CaptchaGenerator.GeneratedCaptcha(
                "ABCDE",
                "data:image/svg+xml;base64,PHN2Zy8+");
        captchaService = new CaptchaService(generator, new InMemoryCaptchaStore(), properties);
    }

    @Test
    @DisplayName("正确验证码只能随业务操作消费一次")
    void validChallengeCanOnlyBeConsumedOnce() {
        String clientKey = "client-a";
        CaptchaChallenge challenge = captchaService.create("EMAIL_CODE", clientKey);

        captchaService.validate(
                challenge.challengeId(),
                "abcde",
                "EMAIL_CODE",
                clientKey);
        BusinessException exception = assertThrows(BusinessException.class, () ->
                captchaService.validate(
                        challenge.challengeId(),
                        "abcde",
                        "EMAIL_CODE",
                        clientKey));
        assertEquals(ErrorCode.CAPTCHA_INVALID, exception.errorCode());
    }

    @Test
    @DisplayName("错误答案和客户端不匹配都会使挑战失效")
    void invalidAnswerOrClientIsRejected() {
        CaptchaChallenge challenge = captchaService.create("LOGIN", "client-a");

        BusinessException wrongAnswer = assertThrows(BusinessException.class, () ->
                captchaService.validate(
                        challenge.challengeId(),
                        "ZZZZZ",
                        "LOGIN",
                        "client-a"));
        assertEquals(ErrorCode.CAPTCHA_INVALID, wrongAnswer.errorCode());

        CaptchaChallenge secondChallenge = captchaService.create("LOGIN", "client-a");
        BusinessException wrongClient = assertThrows(BusinessException.class, () ->
                captchaService.validate(
                        secondChallenge.challengeId(),
                        "ABCDE",
                        "LOGIN",
                        "client-b"));
        assertEquals(ErrorCode.CAPTCHA_INVALID, wrongClient.errorCode());
    }
}
