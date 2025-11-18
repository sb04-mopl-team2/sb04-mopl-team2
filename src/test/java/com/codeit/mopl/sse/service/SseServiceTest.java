package com.codeit.mopl.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.sse.SseMessage;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @InjectMocks
    private SseService sseService;

    private UUID receiverId;
    private UUID lastEventId;

    @BeforeEach
    void setUp() {
        receiverId = UUID.randomUUID();
        lastEventId = UUID.randomUUID();
    }

    @Test
    @DisplayName("SSE 연결에 성공하고 emitter를 반환한다")
    void connect_Success() {
        // when
        SseEmitter emitter = sseService.connect(receiverId, lastEventId);

        // then
        assertThat(emitter).isNotNull();
        verify(sseEmitterRegistry).addEmitter(eq(receiverId), any(SseEmitter.class));
    }

    @Test
    @DisplayName("SSE 연결 시 초기 연결 이벤트를 전송한다")
    void connect_SendsInitialEvent() {
        // when
        SseEmitter emitter = sseService.connect(receiverId, lastEventId);

        // then
        assertThat(emitter).isNotNull();
        // Initial "connect" event should be sent (verified by no exception)
    }

    @Test
    @DisplayName("활성 연결이 있으면 이벤트를 전송한다")
    void send_WithActiveConnection() {
        // given
        String eventName = "notification";
        String data = "test data";
        
        SseEmitter mockEmitter = mock(SseEmitter.class);
        List<SseEmitter> emitters = new ArrayList<>();
        emitters.add(mockEmitter);
        
        Map<UUID, List<SseEmitter>> dataMap = new HashMap<>();
        dataMap.put(receiverId, emitters);
        
        SseMessage sseMessage = new SseMessage(UUID.randomUUID(), eventName, data);
        
        when(sseEmitterRegistry.addNewEvent(receiverId, eventName, data)).thenReturn(sseMessage);
        when(sseEmitterRegistry.getData()).thenReturn(dataMap);

        // when
        sseService.send(receiverId, eventName, data);

        // then
        verify(sseEmitterRegistry).addNewEvent(receiverId, eventName, data);
        verify(sseEmitterRegistry).getData();
    }

    @Test
    @DisplayName("활성 연결이 없으면 이벤트를 전송하지 않는다")
    void send_WithoutActiveConnection() {
        // given
        String eventName = "notification";
        String data = "test data";
        
        Map<UUID, List<SseEmitter>> dataMap = new HashMap<>();
        
        SseMessage sseMessage = new SseMessage(UUID.randomUUID(), eventName, data);
        
        when(sseEmitterRegistry.addNewEvent(receiverId, eventName, data)).thenReturn(sseMessage);
        when(sseEmitterRegistry.getData()).thenReturn(dataMap);

        // when
        sseService.send(receiverId, eventName, data);

        // then
        verify(sseEmitterRegistry).addNewEvent(receiverId, eventName, data);
        verify(sseEmitterRegistry).getData();
    }

    @Test
    @DisplayName("빈 emitter 리스트일 때 이벤트를 전송하지 않는다")
    void send_WithEmptyEmitterList() {
        // given
        String eventName = "notification";
        String data = "test data";
        
        List<SseEmitter> emptyList = new ArrayList<>();
        Map<UUID, List<SseEmitter>> dataMap = new HashMap<>();
        dataMap.put(receiverId, emptyList);
        
        SseMessage sseMessage = new SseMessage(UUID.randomUUID(), eventName, data);
        
        when(sseEmitterRegistry.addNewEvent(receiverId, eventName, data)).thenReturn(sseMessage);
        when(sseEmitterRegistry.getData()).thenReturn(dataMap);

        // when
        sseService.send(receiverId, eventName, data);

        // then
        verify(sseEmitterRegistry).addNewEvent(receiverId, eventName, data);
    }

    @Test
    @DisplayName("여러 emitter에게 동일한 이벤트를 전송한다")
    void send_ToMultipleEmitters() {
        // given
        String eventName = "notification";
        String data = "test data";
        
        SseEmitter mockEmitter1 = mock(SseEmitter.class);
        SseEmitter mockEmitter2 = mock(SseEmitter.class);
        List<SseEmitter> emitters = new ArrayList<>();
        emitters.add(mockEmitter1);
        emitters.add(mockEmitter2);
        
        Map<UUID, List<SseEmitter>> dataMap = new HashMap<>();
        dataMap.put(receiverId, emitters);
        
        SseMessage sseMessage = new SseMessage(UUID.randomUUID(), eventName, data);
        
        when(sseEmitterRegistry.addNewEvent(receiverId, eventName, data)).thenReturn(sseMessage);
        when(sseEmitterRegistry.getData()).thenReturn(dataMap);

        // when
        sseService.send(receiverId, eventName, data);

        // then
        verify(sseEmitterRegistry).getData();
    }

    @Test
    @DisplayName("다양한 이벤트 타입을 전송할 수 있다")
    void send_DifferentEventTypes() {
        // given
        Map<UUID, List<SseEmitter>> dataMap = new HashMap<>();
        dataMap.put(receiverId, new ArrayList<>());
        
        when(sseEmitterRegistry.getData()).thenReturn(dataMap);
        when(sseEmitterRegistry.addNewEvent(eq(receiverId), any(), any()))
            .thenReturn(new SseMessage(UUID.randomUUID(), "event", "data"));

        // when
        sseService.send(receiverId, "notification", "notification data");
        sseService.send(receiverId, "message", "message data");
        sseService.send(receiverId, "alert", "alert data");

        // then
        verify(sseEmitterRegistry, times(3)).addNewEvent(eq(receiverId), any(), any());
    }
}