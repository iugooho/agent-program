package com.travelagent.travelplanner.chat.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 实时对话配置。
 *
 * <p>模型供应商、上下文窗口、超时和线程池都从配置读取，
 * 换模型或调整流式参数不需要改业务代码。</p>
 *
 * @param providers 模型候选链，按顺序选择第一个可用的实现
 * @param defaultTitle 新会话默认标题
 * @param systemPrompt 系统提示，支持 {{today}} 占位符
 * @param contextMessages 最多带入的历史消息条数
 * @param contextCharBudget 历史消息总字符预算
 * @param titleMaxLength 自动生成标题的最大长度
 * @param previewMaxLength 会话列表摘要最大长度
 * @param maxContentChars 单条用户消息最大字符数
 * @param streamTimeout SSE 连接超时
 * @param heartbeatInterval SSE 心跳间隔
 * @param replayRetention 完成后保留内存回放缓冲的时长
 * @param flushIntervalMs 流式过程中落库部分内容的最小间隔
 * @param maxBufferedChunks 单个回复最多保留的增量块数
 * @param fallbackEnabled 主模型失败时是否降级到规则回复
 * @param ruleChunkDelay 规则降级模式的增量间隔，仅用于本地演示，可设为 0
 * @param executor 生成线程池参数
 */
@ConfigurationProperties(prefix = "app.chat")
public record ChatProperties(
        List<String> providers,
        String defaultTitle,
        String systemPrompt,
        int contextMessages,
        int contextCharBudget,
        int titleMaxLength,
        int previewMaxLength,
        int maxContentChars,
        Duration streamTimeout,
        Duration heartbeatInterval,
        Duration replayRetention,
        long flushIntervalMs,
        int maxBufferedChunks,
        boolean fallbackEnabled,
        Duration ruleChunkDelay,
        Executor executor) {

    /**
     * 生成线程池参数。
     *
     * @param coreSize 核心线程数
     * @param maxSize 最大线程数
     * @param queueCapacity 队列容量
     */
    public record Executor(int coreSize, int maxSize, int queueCapacity) {
    }
}
