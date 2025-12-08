package com.codeit.mopl.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.codeit.mopl.sse.SseMessage;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.codeit.mopl.sse.service.SseService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

  @Mock
  private SseEmitterRegistry sseEmitterRegistry;

  @Spy
  @InjectMocks
  private SseService sseService;

  @Test
  @DisplayName("connect - lastEventId가 null이면 emitter를 등록하고 reSend를 호출하지 않는다")
  void connect_whenLastEventIdIsNull_registersEmitterAndDoesNotCallReSend() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID lastEventId = null;

    ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);

    // when
    SseEmitter result = sseService.connect(receiverId, lastEventId);

    // then
    assertNotNull(result);

    verify(sseEmitterRegistry).addEmitter(eq(receiverId), emitterCaptor.capture());
    SseEmitter registered = emitterCaptor.getValue();

    assertSame(result, registered);
    verify(sseService, never()).reSend(any(UUID.class), any(UUID.class), any(SseEmitter.class));
  }

  @Test
  @DisplayName("connect - lastEventId가 null이 아니면 reSend를 호출한다")
  void connect_whenLastEventIdIsNotNull_callsReSend() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID lastEventId = UUID.randomUUID();

    doNothing().when(sseService)
        .reSend(eq(receiverId), eq(lastEventId), any(SseEmitter.class));

    // when
    SseEmitter result = sseService.connect(receiverId, lastEventId);

    // then
    assertNotNull(result);
    verify(sseEmitterRegistry).addEmitter(eq(receiverId), any(SseEmitter.class));
    verify(sseService).reSend(eq(receiverId), eq(lastEventId), any(SseEmitter.class));
  }

  @Test
  @DisplayName("send - 활성 emitter가 없으면 이벤트만 저장하고 전송은 하지 않는다")
  void send_whenNoEmitters_onlySavesEvent() {
    // given
    UUID receiverId = UUID.randomUUID();
    String eventName = "test-event";
    String data = "payload";

    SseMessage message = mock(SseMessage.class);
    when(sseEmitterRegistry.addNewEvent(receiverId, eventName, data))
        .thenReturn(message);

    // getData() 리턴 타입: ConcurrentMap<UUID, List<SseEmitter>>
    when(sseEmitterRegistry.getData())
        .thenReturn(new ConcurrentHashMap<>());

    // when
    sseService.send(receiverId, eventName, data);

    // then
    verify(sseEmitterRegistry).addNewEvent(receiverId, eventName, data);
    verify(sseEmitterRegistry).getData();
    verify(sseEmitterRegistry, never()).removeEmitter(any(), any());
  }

  @Test
  @DisplayName("send - 정상 emitter가 있으면 이벤트를 전송한다")
  void send_withHealthyEmitters_sendsEvent() throws Exception {
    // given
    UUID receiverId = UUID.randomUUID();
    String eventName = "test-event";
    String data = "payload";

    UUID eventId = UUID.randomUUID();
    SseMessage message = mock(SseMessage.class);
    when(message.getEventId()).thenReturn(eventId);
    when(message.getEventName()).thenReturn(eventName);
    when(message.getData()).thenReturn(data);

    when(sseEmitterRegistry.addNewEvent(receiverId, eventName, data))
        .thenReturn(message);

    TestEmitter healthyEmitter = new TestEmitter(false); // 예외 던지지 않는 emitter

    ConcurrentMap<UUID, List<SseEmitter>> map = new ConcurrentHashMap<>();
    map.put(receiverId, new CopyOnWriteArrayList<>(List.of(healthyEmitter)));

    when(sseEmitterRegistry.getData()).thenReturn(map);

    // when
    sseService.send(receiverId, eventName, data);

    // then
    assertEquals(1, healthyEmitter.sendCount);
    verify(sseEmitterRegistry, never()).removeEmitter(eq(receiverId), eq(healthyEmitter));
  }

  @Test
  @DisplayName("send - IOException 발생 시 emitter를 제거한다")
  void send_whenIOException_removesEmitter() throws Exception {
    // given
    UUID receiverId = UUID.randomUUID();
    String eventName = "test-event";
    String data = "payload";

    // ❗ 여기에서 SseMessage의 필드도 스텁해줘야 함
    UUID eventId = UUID.randomUUID();
    SseMessage message = mock(SseMessage.class);
    when(message.getEventId()).thenReturn(eventId);
    when(message.getEventName()).thenReturn(eventName);
    when(message.getData()).thenReturn(data);

    when(sseEmitterRegistry.addNewEvent(receiverId, eventName, data))
        .thenReturn(message);

    TestEmitter brokenEmitter = new TestEmitter(true); // send 시 예외 던짐

    ConcurrentMap<UUID, List<SseEmitter>> map = new ConcurrentHashMap<>();
    map.put(receiverId, new CopyOnWriteArrayList<>(List.of(brokenEmitter)));

    when(sseEmitterRegistry.getData()).thenReturn(map);

    // when
    sseService.send(receiverId, eventName, data);

    // then
    // IOException 발생 전까지 send()는 한 번 호출되어야 함
    assertEquals(1, brokenEmitter.sendCount);
    verify(sseEmitterRegistry).removeEmitter(receiverId, brokenEmitter);
  }

  @Test
  @DisplayName("reSend - 신규 이벤트가 없으면 전송하지 않는다")
  void reSend_whenNoNewEvents_doesNothing() throws Exception {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID lastEventId = UUID.randomUUID();

    when(sseEmitterRegistry.getNewEvents(receiverId, lastEventId))
        .thenReturn(List.of());

    TestEmitter emitter = new TestEmitter(false);

    // when
    sseService.reSend(receiverId, lastEventId, emitter);

    // then
    verify(sseEmitterRegistry).getNewEvents(receiverId, lastEventId);
    assertEquals(0, emitter.sendCount);
    verify(sseEmitterRegistry, never()).removeEmitter(any(), any());
  }

  @Test
  @DisplayName("reSend - 신규 이벤트가 있으면 순차적으로 재전송한다")
  void reSend_withNewEvents_sendsAll() throws Exception {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID lastEventId = UUID.randomUUID();

    SseMessage m1 = mock(SseMessage.class);
    SseMessage m2 = mock(SseMessage.class);

    when(m1.getEventId()).thenReturn(UUID.randomUUID());
    when(m2.getEventId()).thenReturn(UUID.randomUUID());

    when(sseEmitterRegistry.getNewEvents(receiverId, lastEventId))
        .thenReturn(List.of(m1, m2));

    TestEmitter emitter = new TestEmitter(false);

    // when
    sseService.reSend(receiverId, lastEventId, emitter);

    // then
    assertEquals(2, emitter.sendCount);
    verify(sseEmitterRegistry, never()).removeEmitter(any(), any());
  }

  @Test
  @DisplayName("reSend - 예외 발생 시 emitter를 제거한다")
  void reSend_whenException_removesEmitter() throws Exception {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID lastEventId = UUID.randomUUID();

    SseMessage m1 = mock(SseMessage.class);
    when(m1.getEventId()).thenReturn(UUID.randomUUID());

    when(sseEmitterRegistry.getNewEvents(receiverId, lastEventId))
        .thenReturn(List.of(m1));

    TestEmitter brokenEmitter = new TestEmitter(true);

    // when
    sseService.reSend(receiverId, lastEventId, brokenEmitter);

    // then
    assertEquals(1, brokenEmitter.sendCount);
    verify(sseEmitterRegistry).removeEmitter(receiverId, brokenEmitter);
  }

  @Test
  @DisplayName("cleanUp - ping 실패 emitter는 complete 후 제거한다")
  void cleanUp_mixedEmitters() throws Exception {
    // given
    UUID receiverId = UUID.randomUUID();

    TestEmitter healthy = new TestEmitter(false); // ping 성공
    TestEmitter broken = new TestEmitter(true);   // ping 중 예외

    ConcurrentMap<UUID, List<SseEmitter>> map = new ConcurrentHashMap<>();
    map.put(receiverId, new CopyOnWriteArrayList<>(List.of(healthy, broken)));

    when(sseEmitterRegistry.getData()).thenReturn(map);

    // when
    sseService.cleanUp();

    // then
    assertEquals(1, healthy.sendCount);
    assertEquals(1, broken.sendCount);

    org.junit.jupiter.api.Assertions.assertTrue(broken.completeCalled);
    verify(sseEmitterRegistry).removeEmitter(receiverId, broken);

    org.junit.jupiter.api.Assertions.assertFalse(healthy.completeCalled);
    verify(sseEmitterRegistry, never()).removeEmitter(receiverId, healthy);
  }

  @Test
  @DisplayName("SseCloseReason enum 값들이 기대한 이름과 순서로 존재한다")
  void enum_values_shouldMatchExpectedOrder() {
    SseCloseReason[] values = SseCloseReason.values();

    assertEquals(4, values.length);
    assertEquals(SseCloseReason.CLIENT_CLOSED, values[0]);
    assertEquals(SseCloseReason.TIMEOUT, values[1]);
    assertEquals(SseCloseReason.ERROR, values[2]);
    assertEquals(SseCloseReason.SERVER_COMPLETE, values[3]);

    // valueOf 도 한 번 써주면 커버리지 더 올라감
    assertNotNull(SseCloseReason.valueOf("CLIENT_CLOSED"));
    assertNotNull(SseCloseReason.valueOf("TIMEOUT"));
    assertNotNull(SseCloseReason.valueOf("ERROR"));
    assertNotNull(SseCloseReason.valueOf("SERVER_COMPLETE"));
  }

  static class TestEmitter extends SseEmitter {
    int sendCount = 0;
    boolean completeCalled = false;
    final boolean throwOnSend;

    TestEmitter(boolean throwOnSend) {
      super(60_000L);
      this.throwOnSend = throwOnSend;
    }

    @Override
    public void send(SseEventBuilder builder) throws IOException {
      sendCount++;
      if (throwOnSend) {
        throw new IOException("test send error");
      }
    }

    @Override
    public void complete() {
      completeCalled = true;
      super.complete();
    }
  }
}
