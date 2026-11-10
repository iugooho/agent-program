package com.travelagent.travelplanner.auth.infrastructure.mail;

import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** 本地开发用验证码出口；写入 target/local-email-outbox.log，不把验证码写进应用日志。 */
public class LocalEmailCodeSender implements EmailCodeSender {

    private final Path outbox;

    /**
     * 创建本地发送器。
     *
     * @param outbox 本地发件箱路径
     */
    public LocalEmailCodeSender(Path outbox) {
        this.outbox = outbox;
    }

    @Override
    public void send(String email, EmailPurpose purpose, String code) {
        String line = Instant.now() + " | " + purpose + " | " + email + " | " + code + System.lineSeparator();
        try {
            Files.createDirectories(outbox.getParent());
            Files.writeString(outbox, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new IllegalStateException("无法写入本地邮件发件箱", exception);
        }
    }
}
