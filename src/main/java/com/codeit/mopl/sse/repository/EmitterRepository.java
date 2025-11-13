package com.codeit.mopl.sse.repository;

import com.codeit.mopl.sse.SseMessage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class EmitterRepository {

  @Getter
  private final ConcurrentMap<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();
  // UUID: 수신자의 ID

  private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();

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
    SseMessage sseMessage = new SseMessage(eventId, receiverId, eventName, eventData, Instant.now());

    eventIdQueue.addLast(eventId);
    messages.put(eventId, sseMessage);
    return sseMessage;
  }

  public Collection<UUID> getAllUserId(){
    return data.keySet();
  }
}
