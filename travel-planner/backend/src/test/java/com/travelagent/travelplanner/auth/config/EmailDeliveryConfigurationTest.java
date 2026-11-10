package com.travelagent.travelplanner.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** 邮箱投递配置文件测试。 */
class EmailDeliveryConfigurationTest {

    private final EmailDeliveryConfiguration configuration = new EmailDeliveryConfiguration();

    @Test
    @DisplayName("网易 SMTP 使用 465 端口、SSL 和超时配置")
    void configuresNetEaseSmtp() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) configuration.smtpJavaMailSender(
                properties("authorization-code"));

        assertEquals("smtp.163.com", sender.getHost());
        assertEquals(465, sender.getPort());
        assertEquals("sender@163.com", sender.getUsername());
        assertEquals("UTF-8", sender.getDefaultEncoding());
        assertEquals("true", sender.getJavaMailProperties().getProperty("mail.smtp.auth"));
        assertEquals("true", sender.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
        assertEquals("false", sender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
        assertEquals("10000", sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"));
        assertEquals("30000", sender.getJavaMailProperties().getProperty("mail.smtp.timeout"));
        assertEquals("30000", sender.getJavaMailProperties().getProperty("mail.smtp.writetimeout"));
    }

    @Test
    @DisplayName("SMTP 凭据缺失时只报告配置字段名")
    void rejectsMissingPasswordWithoutEchoingValue() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.smtpJavaMailSender(properties("")));

        assertTrue(exception.getMessage().contains("app.auth.email.password"));
    }

    private AuthProperties properties(String password) {
        AuthProperties.Email email = new AuthProperties.Email(
                "smtp",
                "smtp.163.com",
                465,
                "sender@163.com",
                password,
                "sender@163.com",
                true,
                false,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(30));
        return new AuthProperties(
                null,
                null,
                null,
                email,
                new AuthProperties.Captcha(
                        Duration.ofMinutes(2),
                        3,
                        Duration.ofMinutes(10)),
                new AuthProperties.RateLimit(true, false, 120, 60, 20));
    }
}
