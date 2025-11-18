package com.codeit.mopl.sse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mopl.sse.SseMessage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRegistryTest {

    private SseEmitterRegistry sseEmitterRegistry;
    private UUID receiverId;

    @BeforeEach
    void setUp() {
        sseEmitterRegistry = new SseEmitterRegistry();
        receiverId = UUID.randomUUID();
    }

    @Test
    @DisplayName("emitter를 추가할 수 있다")
    void addEmitter_Success() {
        // given
        SseEmitter emitter = new SseEmitter();

        // when
        sseEmitterRegistry.addEmitter(receiverId, emitter);

        // then
        List<SseEmitter> emitters = sseEmitterRegistry.getData().get(receiverId);
        assertThat(emitters).isNotNull();
        assertThat(emitters).contains(emitter);
    }

    @Test
    @DisplayName("동일한 사용자에 대해 여러 emitter를 추가할 수 있다")
    void addEmitter_MultipleEmitters() {
        // given
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();

        // when
        sseEmitterRegistry.addEmitter(receiverId, emitter1);
        sseEmitterRegistry.addEmitter(receiverId, emitter2);

        // then
        List<SseEmitter> emitters = sseEmitterRegistry.getData().get(receiverId);
        assertThat(emitters).hasSize(2);
        assertThat(emitters).containsExactly(emitter1, emitter2);
    }

    @Test
    @DisplayName("emitter를 제거할 수 있다")
    void removeEmitter_Success() {
        // given
        SseEmitter emitter = new SseEmitter();
        sseEmitterRegistry.addEmitter(receiverId, emitter);

        // when
        sseEmitterRegistry.removeEmitter(receiverId, emitter);

        // then
        List<SseEmitter> emitters = sseEmitterRegistry.getData().get(receiverId);
        assertThat(emitters).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 emitter 제거 시 오류가 발생하지 않는다")
    void removeEmitter_NonExistent() {
        // given
        SseEmitter emitter = new SseEmitter();

        // when & then - should not throw exception
        sseEmitterRegistry.removeEmitter(receiverId, emitter);
    }

    @Test
    @DisplayName("새 이벤트를 추가하고 반환한다")
    void addNewEvent_Success() {
        // given
        String eventName = "notification";
        Object data = "test data";

        // when
        SseMessage result = sseEmitterRegistry.addNewEvent(receiverId, eventName, data);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isNotNull();
        assertThat(result.getEventName()).isEqualTo(eventName);
        assertThat(result.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("여러 이벤트를 추가할 수 있다")
    void addNewEvent_MultipleEvents() {
        // when
        SseMessage event1 = sseEmitterRegistry.addNewEvent(receiverId, "event1", "data1");
        SseMessage event2 = sseEmitterRegistry.addNewEvent(receiverId, "event2", "data2");

        // then
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        assertThat(event1.getEventName()).isEqualTo("event1");
        assertThat(event2.getEventName()).isEqualTo("event2");
    }

    @Test
    @DisplayName("getData()로 전체 emitter 맵을 조회할 수 있다")
    void getData_ReturnsAllEmitters() {
        // given
        UUID receiverId1 = UUID.randomUUID();
        UUID receiverId2 = UUID.randomUUID();
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();

        sseEmitterRegistry.addEmitter(receiverId1, emitter1);
        sseEmitterRegistry.addEmitter(receiverId2, emitter2);

        // when
        var data = sseEmitterRegistry.getData();

        // then
        assertThat(data).hasSize(2);
        assertThat(data.get(receiverId1)).contains(emitter1);
        assertThat(data.get(receiverId2)).contains(emitter2);
    }
}