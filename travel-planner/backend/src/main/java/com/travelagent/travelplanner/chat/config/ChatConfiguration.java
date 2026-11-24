package com.travelagent.travelplanner.chat.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 对话模块的线程池与配置装配。 */
@Configuration
@EnableConfigurationProperties(ChatProperties.class)
public class ChatConfiguration {

    /**
     * 生成回复的线程池。模型调用是阻塞式 IO，不能占用 Web 请求线程。
     *
     * @param properties 对话配置
     * @return 线程池
     */
    @Bean(name = "chatExecutor")
    public Executor chatExecutor(ChatProperties properties) {
        ChatProperties.Executor config = properties.executor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, config.coreSize()));
        executor.setMaxPoolSize(Math.max(executor.getCorePoolSize(), config.maxSize()));
        executor.setQueueCapacity(Math.max(1, config.queueCapacity()));
        executor.setThreadNamePrefix("chat-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    /**
     * SSE 心跳调度器。使用守护线程，避免阻止应用退出。
     *
     * @return 调度器
     */
    @Bean(name = "chatHeartbeatScheduler", destroyMethod = "shutdownNow")
    public ScheduledExecutorService chatHeartbeatScheduler() {
        AtomicInteger index = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "chat-heartbeat-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newScheduledThreadPool(1, factory);
    }
}
