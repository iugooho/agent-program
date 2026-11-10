package com.travelagent.travelplanner.auth.config;

import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.infrastructure.mail.AsyncEmailCodeSender;
import com.travelagent.travelplanner.auth.infrastructure.mail.LocalEmailCodeSender;
import com.travelagent.travelplanner.auth.infrastructure.mail.SmtpEmailCodeSender;
import com.travelagent.travelplanner.auth.infrastructure.mail.UnavailableEmailCodeSender;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 邮箱验证码投递配置。 */
@Configuration
public class EmailDeliveryConfiguration {

    /**
     * 本地开发投递器。
     *
     * @return 本地发件箱实现
     */
    @Bean
    @ConditionalOnProperty(name = "app.auth.email.mode", havingValue = "local", matchIfMissing = true)
    public EmailCodeSender localEmailCodeSender() {
        return new LocalEmailCodeSender(Path.of("target", "local-email-outbox.log").toAbsolutePath());
    }

    /**
     * 网易邮箱等 SMTP 服务的 JavaMail 客户端。
     *
     * @param authProperties 认证配置
     * @return SMTP 客户端
     */
    @Bean
    @ConditionalOnProperty(name = "app.auth.email.mode", havingValue = "smtp")
    public JavaMailSender smtpJavaMailSender(AuthProperties authProperties) {
        AuthProperties.Email options = validateSmtpOptions(authProperties.email());
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(options.host().strip());
        sender.setPort(options.port());
        sender.setUsername(options.username().strip());
        sender.setPassword(options.password());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        sender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
        sender.getJavaMailProperties().setProperty(
                "mail.smtp.ssl.enable", Boolean.toString(options.sslEnabled()));
        sender.getJavaMailProperties().setProperty(
                "mail.smtp.starttls.enable", Boolean.toString(options.starttlsEnabled()));
        sender.getJavaMailProperties().setProperty("mail.smtp.ssl.checkserveridentity", "true");
        sender.getJavaMailProperties().setProperty(
                "mail.smtp.connectiontimeout", Long.toString(options.connectTimeout().toMillis()));
        sender.getJavaMailProperties().setProperty(
                "mail.smtp.timeout", Long.toString(options.readTimeout().toMillis()));
        sender.getJavaMailProperties().setProperty(
                "mail.smtp.writetimeout", Long.toString(options.writeTimeout().toMillis()));
        return sender;
    }

    /**
     * 验证码邮件后台投递线程池。
     *
     * @return 有界线程池，避免阻塞认证请求
     */
    @Bean(name = "emailCodeTaskExecutor")
    public TaskExecutor emailCodeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-code-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    /**
     * SMTP 验证码投递器。
     *
     * @param mailSender SMTP 客户端
     * @param authProperties 认证配置
     * @param emailCodeTaskExecutor 验证码邮件后台投递线程池
     * @return SMTP 投递器
     */
    @Bean
    @ConditionalOnProperty(name = "app.auth.email.mode", havingValue = "smtp")
    public EmailCodeSender smtpEmailCodeSender(
            JavaMailSender mailSender,
            AuthProperties authProperties,
            @Qualifier("emailCodeTaskExecutor") TaskExecutor emailCodeTaskExecutor) {
        EmailCodeSender delegate = new SmtpEmailCodeSender(mailSender, authProperties.email().from().strip());
        return new AsyncEmailCodeSender(delegate, emailCodeTaskExecutor);
    }

    /**
     * 未配置生产邮件网关时的兜底实现。
     *
     * @return 显式失败实现
     */
    @Bean
    @ConditionalOnMissingBean(EmailCodeSender.class)
    public EmailCodeSender unavailableEmailCodeSender() {
        return new UnavailableEmailCodeSender();
    }

    private AuthProperties.Email validateSmtpOptions(AuthProperties.Email options) {
        List<String> missing = new ArrayList<>();
        requireText(options.host(), "app.auth.email.host", missing);
        requireText(options.username(), "app.auth.email.username", missing);
        requireText(options.password(), "app.auth.email.password", missing);
        requireText(options.from(), "app.auth.email.from", missing);
        if (options.port() < 1 || options.port() > 65_535) {
            missing.add("app.auth.email.port");
        }
        if (options.connectTimeout() == null) {
            missing.add("app.auth.email.connect-timeout");
        }
        if (options.readTimeout() == null) {
            missing.add("app.auth.email.read-timeout");
        }
        if (options.writeTimeout() == null) {
            missing.add("app.auth.email.write-timeout");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("SMTP 邮件配置不完整或无效：" + String.join(", ", missing));
        }
        if (options.sslEnabled() && options.starttlsEnabled()) {
            throw new IllegalStateException("app.auth.email.ssl-enabled 与 starttls-enabled 不能同时启用");
        }
        return options;
    }

    private void requireText(String value, String field, List<String> missing) {
        if (value == null || value.isBlank()) {
            missing.add(field);
        }
    }
}
