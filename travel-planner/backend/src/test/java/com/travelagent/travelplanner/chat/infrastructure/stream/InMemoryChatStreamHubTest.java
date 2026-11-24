package com.travelagent.travelplanner.chat.infrastructure.stream;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelagent.travelplanner.chat.application.ChatStreamHub;
import com.travelagent.travelplanner.chat.support.ChatTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 内存事件总线的回放与终止语义。 */
class InMemoryChatStreamHubTest {

    @Test
    @DisplayName("订阅先回放历史增量，再接收实时增量")
    void replaysThenStreamsLiveChunks() {
        InMemoryChatStreamHub hub = new InMemoryChatStreamHub(ChatTestSupport.properties());
        hub.begin(10L);
        hub.publish(10L, 1L, "你");
        hub.publish(10L, 2L, "好");

        RecordingListener listener = new RecordingListener();
        ChatStreamHub.Subscription subscription = hub.subscribe(10L, 0L, listener);

        assertThat(listener.chunks).containsExactly("你", "好");
        hub.publish(10L, 3L, "！");
        assertThat(listener.chunks).containsExactly("你", "好", "！");

        hub.complete(10L);
        assertThat(listener.completed).isTrue();
        subscription.close();    }

    @Test
    @DisplayName("按 Last-Event-ID 之后续传")
    void resumesAfterSequence() {
        InMemoryChatStreamHub hub = new InMemoryChatStreamHub(ChatTestSupport.properties());
        hub.publish(11L, 1L, "a");
        hub.publish(11L, 2L, "b");
        hub.publish(11L, 3L, "c");

        RecordingListener listener = new RecordingListener();
        hub.subscribe(11L, 2L, listener);

        assertThat(listener.chunks).containsExactly("c");
    }

    @Test
    @DisplayName("生成失败会推送错误并结束通道")
    void deliversFailure() {
        InMemoryChatStreamHub hub = new InMemoryChatStreamHub(ChatTestSupport.properties());
        hub.begin(12L);
        RecordingListener listener = new RecordingListener();
        hub.subscribe(12L, 0L, listener);

        hub.fail(12L, 4002, "模型不可用");

        assertThat(listener.errorCode).isEqualTo(4002);
        assertThat(listener.errorMessage).isEqualTo("模型不可用");
        assertThat(hub.isActive(12L)).isFalse();
    }

    private static final class RecordingListener implements ChatStreamHub.Listener {

        private final List<String> chunks = new ArrayList<>();
        private boolean completed;
        private Integer errorCode;
        private String errorMessage;

        @Override
        public void onChunk(long sequence, String content) {
            chunks.add(content);
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        public void onError(int code, String message) {
            errorCode = code;
            errorMessage = message;
        }
    }
}
