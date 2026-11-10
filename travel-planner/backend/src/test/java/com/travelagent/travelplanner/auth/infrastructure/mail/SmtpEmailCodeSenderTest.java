package com.travelagent.travelplanner.auth.infrastructure.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;

/** SMTP 验证码邮件内容单元测试。 */
class SmtpEmailCodeSenderTest {

    @Test
    @DisplayName("验证码邮件包含收件人、中文主题和 UTF-8 正文")
    void sendsExpectedMessage() throws Exception {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        SmtpEmailCodeSender sender = new SmtpEmailCodeSender(mailSender, "sender@163.com");
        sender.send("user@example.com", EmailPurpose.LOGIN, "123456");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage sent = messageCaptor.getValue();
        assertEquals("user@example.com", sent.getAllRecipients()[0].toString());
        assertEquals("sender@163.com", sent.getFrom()[0].toString());
        assertEquals("【旅游规划智能体】登录验证码", sent.getSubject());
        assertTrue(sent.getContent().toString().contains("123456"));
        assertTrue(sent.getContent().toString().contains("登录账号"));
    }
}
