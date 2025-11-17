package com.codeit.mopl.event.listener;

import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(NotificationCreateEvent event) {
    String key = Optional.ofNullable(event.notificationDto().id())
        .map(Object::toString)
        .orElse(null);

    send("mopl-notification-create", key, event);
  }

  private void send(String topic, String key, Object payload) {
    try {
      String json = objectMapper.writeValueAsString(payload);

      String traceId = Optional.ofNullable(MDC.get("requestId")).orElse("N/A");
      ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);
      record.headers().add(new RecordHeader("x-trace-id", traceId.getBytes(StandardCharsets.UTF_8)));
      record.headers().add(new RecordHeader("x-event-type",
          payload.getClass().getSimpleName().getBytes(StandardCharsets.UTF_8)));

      kafkaTemplate.send(record).whenComplete((result, ex) -> {
        if (ex != null) {
          log.warn("[Kafka] 전송 실패 topic={}, key={}, error={}", topic, key, ex.getMessage(), ex);
        } else {
          log.info("[Kafka] 전송 성공 topic={}, key={}, partition={}, offset={}",
              topic, key,
              result.getRecordMetadata().partition(),
              result.getRecordMetadata().offset());
        }
      });
    } catch (JsonProcessingException e) {
      log.warn("[Kafka] 이벤트 직렬화 실패 topic={}, error={}", topic, e.getMessage(), e);
    }
  }
}