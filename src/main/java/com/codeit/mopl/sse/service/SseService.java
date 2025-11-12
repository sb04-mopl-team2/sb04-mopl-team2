package com.codeit.mopl.sse.service;

import com.codeit.mopl.sse.SseMessage;
import com.codeit.mopl.sse.repository.EmitterRepository;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties.Sse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

  private final EmitterRepository emitterRepository;

  // 30분짜리 emitter
  private static final long DEFAULT_TIMEOUT = 1000L * 60 * 30;

  public SseEmitter connect(UUID receiverId, UUID lastEventId) {
    log.info("[SSE CONNECT] receiverId={}, lastEventId={}", receiverId, lastEventId);

    SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

    // 저장
    emitterRepository.addEmitter(receiverId, emitter);
    log.info("[SSE REGISTERED] receiverId={}, currentEmitters={}",
        receiverId, emitterRepository.getData().get(receiverId).size());

    // 해당 emitter 가 추후에 끊기거나 시간이 만료되면 자동으로 정리
    emitter.onCompletion(() -> {
      emitterRepository.removeEmitter(receiverId, emitter);
      log.info("[SSE COMPLETED] receiverId={}", receiverId);
    });
    emitter.onTimeout(() -> {
      emitterRepository.removeEmitter(receiverId, emitter);
      log.warn("[SSE TIMEOUT] receiverId={}", receiverId);
    });

    // 연결 직후 더미 이벤트 한 번 보내기
    try {
      String eventId = UUID.randomUUID().toString();
      emitter.send(SseEmitter.event()
          .id(eventId)
          .name("connect")
          .data("connected"));
      log.info("[SSE CONNECT EVENT SENT] receiverId={}, eventId={}", receiverId, eventId);
    } catch (Exception e) {
      log.error("[SSE CONNECT EVENT FAILED] receiverId={}, error={}", receiverId, e.getMessage());
      emitterRepository.removeEmitter(receiverId, emitter);
    }

    return emitter;
  }

  public void send(Collection<UUID> receiverIds, String eventName, Object data) {
    for (UUID receiverId : receiverIds) {
      SseMessage saved = emitterRepository.addNewEvent(receiverId, eventName, data);

      List<SseEmitter> emitters = emitterRepository.getData().get(receiverId);
      if (emitters == null || emitters.isEmpty()) {
        log.debug("[SSE SEND] no active connection. receiverId={}", receiverId);
        continue;
      }

      for (SseEmitter emitter : emitters) {
        try {
          emitter.send(
              SseEmitter.event()
                  .id(saved.getEventId().toString())
                  .name(saved.getEventName())
                  .data(saved.getData())
          );

          log.debug("[SSE SEND SUCCESS] receiverId={}, eventId={}, eventName={}",
              receiverId, saved.getEventId(), saved.getEventName());

        } catch (Exception e) {
          log.warn("[SSE SEND FAILED] receiverId={}, reason={}", receiverId, e.getMessage());
          emitterRepository.removeEmitter(receiverId, emitter);
        }
      }
    }
  }

  public void broadcast(String eventName, Object data) {
    Set<UUID> allUserIds = emitterRepository.getData().keySet();
    if (allUserIds.isEmpty()) {
      log.debug("[SSE BROADCAST] no active connections. skipped. eventName={}", eventName);
      return;
    }

    log.debug("[SSE BROADCAST] start. eventName={}", eventName);
    send(allUserIds, eventName, data);
  }

  @Scheduled(fixedDelay = 1000 * 60 * 30) // 30분마다 실행
  public void cleanUp() {
    log.debug("[SSE CLEANUP] start");

    for (Map.Entry<UUID, List<SseEmitter>> entry : emitterRepository.getData().entrySet()) {
      UUID receiverId = entry.getKey();
      List<SseEmitter> emitters = entry.getValue();

      if (emitters == null || emitters.isEmpty()) {
        continue;
      }

      for (SseEmitter emitter : emitters) {
        if (!ping(emitter)) {
          emitterRepository.removeEmitter(receiverId, emitter);
        }
      }
    }

    log.debug("[SSE CLEANUP] done");
  }

  private boolean ping(SseEmitter sseEmitter) {
    try {
      sseEmitter.send(
          SseEmitter.event()
              .name("ping")
              .data("keep-alive")
              .reconnectTime(10_000L)
      );
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
