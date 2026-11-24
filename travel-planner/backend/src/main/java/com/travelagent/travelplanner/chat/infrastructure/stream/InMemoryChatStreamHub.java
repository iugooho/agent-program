package com.travelagent.travelplanner.chat.infrastructure.stream;

import com.travelagent.travelplanner.chat.application.ChatStreamHub;
import com.travelagent.travelplanner.chat.config.ChatProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 单机内存版对话事件总线。
 *
 * <p>每个助手消息维护一个带序号的增量缓冲，订阅时先回放再实时推送，
 * 因此浏览器断线重连后可以用 Last-Event-ID 续传，不会丢失中间内容。
 * 生成完成后再保留一段时间，供晚到的订阅者回放，之后自动清理。</p>
 */
@Component
public class InMemoryChatStreamHub implements ChatStreamHub {

    private static final Logger log = LoggerFactory.getLogger(InMemoryChatStreamHub.class);

    private final ConcurrentMap<Long, Channel> channels = new ConcurrentHashMap<>();
    private final int maxBufferedChunks;
    private final long retentionMillis;

    /**
     * 创建内存事件总线。
     *
     * @param properties 对话配置
     */
    public InMemoryChatStreamHub(ChatProperties properties) {
        this.maxBufferedChunks = Math.max(100, properties.maxBufferedChunks());
        Duration retention = properties.replayRetention();
        this.retentionMillis = retention == null ? 120_000L : retention.toMillis();
    }

    @Override
    public void begin(long messageId) {
        channels.computeIfAbsent(messageId, ignored -> new Channel(maxBufferedChunks));
        pruneExpired();
    }

    @Override
    public void publish(long messageId, long sequence, String content) {
        Channel channel = channels.computeIfAbsent(messageId, ignored -> new Channel(maxBufferedChunks));
        channel.publish(sequence, content);
        pruneExpired();
    }

    @Override
    public void complete(long messageId) {
        Channel channel = channels.get(messageId);
        if (channel != null) {
            channel.complete();
        }
        pruneExpired();
    }

    @Override
    public void fail(long messageId, int code, String message) {
        Channel channel = channels.get(messageId);
        if (channel != null) {
            channel.fail(code, message);
        }
        pruneExpired();
    }

    @Override
    public boolean isActive(long messageId) {
        Channel channel = channels.get(messageId);
        return channel != null && !channel.finished();
    }

    @Override
    public Subscription subscribe(long messageId, long afterSequence, Listener listener) {
        Channel channel = channels.computeIfAbsent(messageId, ignored -> new Channel(maxBufferedChunks));
        Subscription subscription = channel.subscribe(afterSequence, listener);
        pruneExpired();
        return subscription;
    }

    private void pruneExpired() {
        long now = System.currentTimeMillis();
        channels.entrySet().removeIf(entry -> entry.getValue().expired(now, retentionMillis));
    }

    private record Chunk(long sequence, String content) {
    }

    private static final class Channel {

        private final int maxBufferedChunks;
        private final List<Chunk> chunks = new ArrayList<>();
        private final List<Listener> listeners = new ArrayList<>();
        private boolean finished;
        private Integer errorCode;
        private String errorMessage;
        private long finishedAtMillis;

        private Channel(int maxBufferedChunks) {
            this.maxBufferedChunks = maxBufferedChunks;
        }

        private synchronized void publish(long sequence, String content) {
            if (finished) {
                return;
            }
            chunks.add(new Chunk(sequence, content));
            if (chunks.size() > maxBufferedChunks) {
                chunks.subList(0, chunks.size() - maxBufferedChunks).clear();
            }
            for (Listener listener : List.copyOf(listeners)) {
                try {
                    listener.onChunk(sequence, content);
                } catch (RuntimeException exception) {
                    log.debug("对话增量监听器异常，已忽略", exception);
                }
            }
        }

        private synchronized void complete() {
            if (finished) {
                return;
            }
            finished = true;
            finishedAtMillis = System.currentTimeMillis();
            for (Listener listener : List.copyOf(listeners)) {
                try {
                    listener.onComplete();
                } catch (RuntimeException exception) {
                    log.debug("对话完成监听器异常，已忽略", exception);
                }
            }
            listeners.clear();
        }

        private synchronized void fail(int code, String message) {
            if (finished) {
                return;
            }
            finished = true;
            errorCode = code;
            errorMessage = message;
            finishedAtMillis = System.currentTimeMillis();
            for (Listener listener : List.copyOf(listeners)) {
                try {
                    listener.onError(code, message);
                } catch (RuntimeException exception) {
                    log.debug("对话错误监听器异常，已忽略", exception);
                }
            }
            listeners.clear();
        }

        private synchronized Subscription subscribe(long afterSequence, Listener listener) {
            listeners.add(listener);
            try {
                for (Chunk chunk : chunks) {
                    if (chunk.sequence() > afterSequence) {
                        listener.onChunk(chunk.sequence(), chunk.content());
                    }
                }
                if (finished) {
                    if (errorCode != null) {
                        listener.onError(errorCode, errorMessage);
                    } else {
                        listener.onComplete();
                    }
                    listeners.remove(listener);
                }
            } catch (RuntimeException exception) {
                listeners.remove(listener);
                throw exception;
            }
            return () -> {
                synchronized (Channel.this) {
                    listeners.remove(listener);
                }
            };
        }

        private synchronized boolean finished() {
            return finished;
        }

        private synchronized boolean expired(long now, long retentionMillis) {
            return finished && now - finishedAtMillis > retentionMillis;
        }
    }
}
