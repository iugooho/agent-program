package com.travelagent.travelplanner.auth.infrastructure.mail;

import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/** 将实际投递放到后台线程，避免 SMTP 高延迟拖垮验证码接口。 */
public class AsyncEmailCodeSender implements EmailCodeSender {

    private static final Logger log = LoggerFactory.getLogger(AsyncEmailCodeSender.class);

    private final EmailCodeSender delegate;
    private final TaskExecutor taskExecutor;

    /**
     * 创建异步验证码投递器。
     *
     * @param delegate 实际发送实现
     * @param taskExecutor 后台发送线程池
     */
    public AsyncEmailCodeSender(EmailCodeSender delegate, TaskExecutor taskExecutor) {
        this.delegate = delegate;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void send(String email, EmailPurpose purpose, String code) {
        try {
            taskExecutor.execute(() -> sendInBackground(email, purpose, code));
        } catch (TaskRejectedException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件发送任务繁忙，请稍后重试");
        }
    }

    private void sendInBackground(String email, EmailPurpose purpose, String code) {
        try {
            delegate.send(email, purpose, code);
        } catch (RuntimeException exception) {
            // 不记录邮箱、验证码或 SMTP 凭据。
            log.error("验证码邮件后台投递失败，purpose={}", purpose, exception);
        }
    }
}
