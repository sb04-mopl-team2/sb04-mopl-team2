package com.codeit.mopl.sse.service;

import com.codeit.mopl.sse.SseMessage;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

  private final SseEmitterRegistry sseEmitterRegistry;

  // 30분짜리 emitter
  private static final long DEFAULT_TIMEOUT = 1000L * 60 * 30;

  public SseEmitter connect(UUID receiverId, UUID lastEventId) {
    log.info("[SSE] SSE 연결 시작, receiverId = {}, lastEventId = {}", receiverId, lastEventId);

    SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

    // 저장
    sseEmitterRegistry.addEmitter(receiverId, emitter);

    // 해당 emitter 가 추후에 끊기거나 시간이 만료되면 자동으로 정리
    emitter.onCompletion(() -> {
      sseEmitterRegistry.removeEmitter(receiverId, emitter);
      log.info("[SSE] emitter 만료, receiverId = {}, emitter = {}", receiverId, System.identityHashCode(emitter));
    });

    emitter.onTimeout(() -> {
      sseEmitterRegistry.removeEmitter(receiverId, emitter);
      log.warn("[SSE] emitter 시간 만료, receiverId = {}, emitter = {}", receiverId, System.identityHashCode(emitter));
    });

    // 연결 직후 더미 이벤트 한 번 보내기
    try {
      String eventId = UUID.randomUUID().toString();
      emitter.send(SseEmitter.event()
          .name("ping"));
      log.info("[SSE] connect 이벤트 전송 성공, receiverId = {}, eventId = {}", receiverId, eventId);
    } catch (Exception e) {
      log.warn("[SSE] SSE 이벤트 전송 실패, receiverId = {}, errorMessage = {}", receiverId, e.getMessage());
      sseEmitterRegistry.removeEmitter(receiverId, emitter);
    }

    if (lastEventId != null) {
      reSend(receiverId, lastEventId, emitter);
    }

    log.info("[SSE] SSE 연결 종료, receiverId = {}, emitter = {}", receiverId, System.identityHashCode(emitter));
    return emitter;
  }

  public void send(UUID receiverId, String eventName, Object data) {
    log.info("[SSE] SSE 이벤트 전송 시작, receiverId = {}, eventName = {}, data = {}", receiverId, eventName, data);
    SseMessage saved = sseEmitterRegistry.addNewEvent(receiverId, eventName, data);

    List<SseEmitter> emitters = sseEmitterRegistry.getData().get(receiverId);
    if (emitters == null || emitters.isEmpty()) {
      log.debug("[SSE] 활성화된 연결이 없음. receiverId = {}", receiverId);
      log.info("[SSE] SSE 이벤트 전송 종료");
      return;
    }

    for (SseEmitter emitter : List.copyOf(emitters)) {
      try {
        emitter.send(
            SseEmitter.event()
                .id(saved.getEventId().toString())
                .name(saved.getEventName())
                .data(saved.getData())
        );

        log.debug("[SSE] SSE 이벤트 전송 성공 receiverId = {}, eventId = {}, eventName = {}",
            receiverId, saved.getEventId(), saved.getEventName());

      } catch (IOException | IllegalStateException e) {
        // 클라이언트가 탭 닫음 / 네트워크 끊김 같은 정상 종료 케이스
        log.info("[SSE] 클라이언트 연결 종료로 전송 실패, receiverId = {}, eventId = {}, reason = {}",
            receiverId, saved.getEventId(), e.getMessage());
        sseEmitterRegistry.removeEmitter(receiverId, emitter);

      } catch (Exception e) {
        // 진짜 이상한 케이스만 에러로
        log.error("[SSE] 예기치 못한 SSE 전송 예외, receiverId = {}, eventId = {}",
            receiverId, saved.getEventId(), e);
        sseEmitterRegistry.removeEmitter(receiverId, emitter);
      }
      // 여기서 return 하지 말고 다음 emitter들도 계속 시도
    }

    log.info("[SSE] SSE 이벤트 전송 종료");
  }

  public void reSend(UUID receiverId, UUID lastEventId, SseEmitter emitter) {
    log.info("[SSE] SSE 이벤트 재전송 시작, receiverId = {}, lastEventId = {}", receiverId, lastEventId);
    List<SseMessage> newEvents = sseEmitterRegistry.getNewEvents(receiverId, lastEventId);

    if (newEvents.isEmpty()) {
      log.info("[SSE] 재전송할 신규 이벤트 없음, receiverId = {}, lastEventId = {}", receiverId, lastEventId);
      return;
    }

    for (SseMessage message : newEvents) {
      try {
        emitter.send(
            SseEmitter.event()
                .id(message.getEventId().toString())
                .name(message.getEventName())
                .data(message.getData())
        );

        log.debug("[SSE] SSE 이벤트 재전송 성공 receiverId = {}, eventId = {}, eventName = {}",
            receiverId, message.getEventId(), message.getEventName());

      } catch (Exception e) {
        log.warn("[SSE] SSE 이벤트 재전송 실패 receiverId = {}, reason = {}", receiverId, e.getMessage());
        sseEmitterRegistry.removeEmitter(receiverId, emitter);
      }
    }
    log.info("[SSE] SSE 이벤트 재전송 종료, receiverId = {}, lastEventId = {}", receiverId, lastEventId);
  }

  @Scheduled(fixedDelay = 1000 * 60 * 30) // 30분마다 실행
  public void cleanUp() {
    log.debug("[SSE] CLEANUP 시작");

    for (Map.Entry<UUID, List<SseEmitter>> entry : sseEmitterRegistry.getData().entrySet()) {
      UUID receiverId = entry.getKey();
      List<SseEmitter> emitters = entry.getValue();

      if (emitters == null || emitters.isEmpty()) {
        continue;
      }

      for (SseEmitter emitter : emitters) {
        if (!ping(emitter)) {
          emitter.complete();
          sseEmitterRegistry.removeEmitter(receiverId, emitter);
        }
      }
    }

    log.debug("[SSE] CLEANUP 종료");
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
