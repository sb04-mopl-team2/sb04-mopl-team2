package com.codeit.mopl.sse.repository;

import com.codeit.mopl.sse.SseMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterRegistry {

  @Getter
  private final ConcurrentMap<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();
  // UUID: 수신자의 ID

  private final ConcurrentMap<UUID, ConcurrentLinkedDeque<UUID>> queuesByReceiverId = new ConcurrentHashMap<>();

  private final Map<UUID, SseMessage> messages = new ConcurrentHashMap<>();

  public void addEmitter(UUID receiverId, SseEmitter emitter) {
    data.computeIfAbsent(receiverId, id -> new CopyOnWriteArrayList<>())
        .add(emitter);
  }

  public void removeEmitter(UUID receiverId, SseEmitter emitter) {
    List<SseEmitter> emitters = data.get(receiverId);
    if (emitters != null) {
      emitters.remove(emitter);
      if (emitters.isEmpty()) {
        data.remove(receiverId);
      }
    }
  }

  public SseMessage addNewEvent(UUID receiverId, String eventName, Object eventData) {
    UUID eventId = UUID.randomUUID();
    SseMessage sseMessage =
        new SseMessage(eventId, receiverId, eventName, eventData, Instant.now());

    ConcurrentLinkedDeque<UUID> queue =
        queuesByReceiverId.computeIfAbsent(receiverId, id -> new ConcurrentLinkedDeque<>());
    queue.addLast(eventId);

    messages.put(eventId, sseMessage);
    return sseMessage;
  }

  public List<SseMessage> getNewEvents(UUID receiverId, UUID lastEventId) {
    Deque<UUID> eventIdQueue = queuesByReceiverId.get(receiverId);
    List<SseMessage> result = new ArrayList<>();

    if (eventIdQueue == null) {
      return result;
    }

      boolean foundLast = false;

    for (UUID eventId : eventIdQueue) {
      if (!foundLast) {
        if (eventId.equals(lastEventId)) {
          foundLast = true;
        }
        continue;
      }

      // lastEventId 이후 이벤트
      SseMessage message = messages.get(eventId);
      if (message == null) {
        continue;
      }

      if (receiverId.equals(message.getReceiverId())) {
        result.add(message);
      }
    }
    return result;
  }
}
