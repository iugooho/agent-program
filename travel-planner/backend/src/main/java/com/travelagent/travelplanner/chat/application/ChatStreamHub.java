package com.travelagent.travelplanner.chat.application;

/**
 * 对话流事件总线。
 *
 * <p>生成线程只负责把增量写入总线，SSE 连接从总线订阅；接口层因此不需要知道
 * 消息是来自真实大模型、规则降级还是后续的其它实现。当前实现是单机内存版，
 * 接口保持不变即可替换为 Redis Pub/Sub 或 STOMP 广播。</p>
 */
public interface ChatStreamHub {

    /**
     * 在开始生成前建立回放通道，避免客户端先订阅时拿不到后续增量。
     *
     * @param messageId 助手消息主键
     */
    void begin(long messageId);

    /**
     * 发布一段增量文本。
     *
     * @param messageId 助手消息主键
     * @param sequence 从 1 开始递增的序号
     * @param content 增量文本
     */
    void publish(long messageId, long sequence, String content);

    /**
     * 通知订阅者生成完成。
     *
     * @param messageId 助手消息主键
     */
    void complete(long messageId);

    /**
     * 通知订阅者生成失败。
     *
     * @param messageId 助手消息主键
     * @param code 业务错误码
     * @param message 错误提示
     */
    void fail(long messageId, int code, String message);

    /**
     * 是否仍有生成中的通道。
     *
     * @param messageId 助手消息主键
     * @return 生成中返回 true
     */
    boolean isActive(long messageId);

    /**
     * 订阅增量。订阅时会先回放 afterSequence 之后的缓冲，再接收实时增量。
     *
     * @param messageId 助手消息主键
     * @param afterSequence 只接收序号大于该值的增量
     * @param listener 监听器
     * @return 可用于取消订阅的句柄
     */
    Subscription subscribe(long messageId, long afterSequence, Listener listener);

    /** 增量监听器。 */
    interface Listener {

        /**
         * 收到一段增量。
         *
         * @param sequence 序号
         * @param content 文本
         */
        void onChunk(long sequence, String content);

        /** 生成完成。 */
        void onComplete();

        /**
         * 生成失败。
         *
         * @param code 业务错误码
         * @param message 错误提示
         */
        void onError(int code, String message);
    }

    /** 订阅句柄。 */
    interface Subscription {

        /** 取消订阅，可重复调用。 */
        void close();
    }
}
