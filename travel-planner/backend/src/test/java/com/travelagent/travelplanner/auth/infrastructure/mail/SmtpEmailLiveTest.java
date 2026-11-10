package com.travelagent.travelplanner.auth.infrastructure.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.config.EmailDeliveryConfiguration;
import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.javamail.JavaMailSender;

/** 使用真实 SMTP 凭据的邮件投递冒烟测试，仅在显式开启时运行。 */
@EnabledIfEnvironmentVariable(named = "APP_EMAIL_LIVE_SMTP", matches = "true")
class SmtpEmailLiveTest {

    @Test
    @DisplayName("通过网易 SMTP 真实发送验证码邮件")
    void sendsThroughNetEaseSmtp() {
        AuthProperties authProperties = new AuthProperties(
                null,
                null,
                null,
                smtpOptions(),
                new AuthProperties.Captcha(
                        Duration.ofMinutes(2),
                        3,
                        Duration.ofMinutes(10)),
                new AuthProperties.RateLimit(true, false, 120, 60, 20));
        EmailDeliveryConfiguration configuration = new EmailDeliveryConfiguration();
        JavaMailSender mailSender = configuration.smtpJavaMailSender(authProperties);
        EmailCodeSender sender = configuration.smtpEmailCodeSender(
                mailSender, authProperties, task -> task.run());

        assertDoesNotThrow(() -> sender.send(
                requiredEnvironment("APP_EMAIL_TEST_RECIPIENT"), EmailPurpose.LOGIN, "246810"));
    }

    private AuthProperties.Email smtpOptions() {
        return new AuthProperties.Email(
                "smtp",
                requiredEnvironment("APP_EMAIL_HOST"),
                Integer.parseInt(requiredEnvironment("APP_EMAIL_PORT")),
                requiredEnvironment("APP_EMAIL_USERNAME"),
                requiredEnvironment("APP_EMAIL_PASSWORD"),
                requiredEnvironment("APP_EMAIL_FROM"),
                Boolean.parseBoolean(requiredEnvironment("APP_EMAIL_SSL_ENABLED")),
                Boolean.parseBoolean(requiredEnvironment("APP_EMAIL_STARTTLS_ENABLED")),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(30));
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 未配置");
        }
        return value;
    }
}
