package com.travelagent.travelplanner.auth.infrastructure.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskExecutor;

/** 异步验证码投递器单元测试。 */
class AsyncEmailCodeSenderTest {

    @Test
    @DisplayName("验证码接口只提交后台任务，不等待 SMTP 完成")
    void submitsDeliveryWithoutBlockingCaller() {
        EmailCodeSender delegate = mock(EmailCodeSender.class);
        TaskExecutor executor = mock(TaskExecutor.class);
        AsyncEmailCodeSender sender = new AsyncEmailCodeSender(delegate, executor);

        sender.send("user@example.com", EmailPurpose.REGISTER, "123456");

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(task.capture());
        verify(delegate, never()).send(any(), any(), any());

        task.getValue().run();
        verify(delegate).send("user@example.com", EmailPurpose.REGISTER, "123456");
    }

    @Test
    @DisplayName("后台投递异常不会传播到验证码接口")
    void swallowsBackgroundDeliveryFailure() {
        EmailCodeSender delegate = mock(EmailCodeSender.class);
        doThrow(new IllegalStateException("smtp unavailable"))
                .when(delegate).send("user@example.com", EmailPurpose.REGISTER, "123456");
        AsyncEmailCodeSender sender = new AsyncEmailCodeSender(delegate, Runnable::run);

        assertDoesNotThrow(() -> sender.send("user@example.com", EmailPurpose.REGISTER, "123456"));
    }
}
