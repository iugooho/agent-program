package com.travelagent.travelplanner.auth.infrastructure.mail;

import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/** 通过 SMTP 发送邮箱验证码，不记录明文验证码或 SMTP 凭据。 */
public class SmtpEmailCodeSender implements EmailCodeSender {

    private final JavaMailSender mailSender;
    private final String from;

    /**
     * 创建 SMTP 验证码发送器。
     *
     * @param mailSender JavaMail 发送器
     * @param from 发件人地址
     */
    public SmtpEmailCodeSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String email, EmailPurpose purpose, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject(subject(purpose));
            helper.setText(body(purpose, code), false);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件发送失败，请稍后重试");
        }
    }

    private String subject(EmailPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> "【旅游规划智能体】注册验证码";
            case LOGIN -> "【旅游规划智能体】登录验证码";
            case UPDATE_PROFILE -> "【旅游规划智能体】资料修改验证码";
        };
    }

    private String body(EmailPurpose purpose, String code) {
        String action = switch (purpose) {
            case REGISTER -> "注册账号";
            case LOGIN -> "登录账号";
            case UPDATE_PROFILE -> "修改账号资料";
        };
        return """
                您好：

                您正在%s，验证码为：%s

                验证码仅限本人使用，请勿向任何人泄露。如非本人操作，请忽略此邮件。

                旅游规划智能体
                """.formatted(action, code);
    }
}
