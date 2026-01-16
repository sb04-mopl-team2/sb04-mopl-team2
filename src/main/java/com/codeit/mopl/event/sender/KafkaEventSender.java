package com.codeit.mopl.event.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventSender {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            String traceId = Optional.ofNullable(MDC.get("requestId")).orElse("N/A");
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);
            record.headers().add(new RecordHeader("x-trace-id", traceId.getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("x-event-type",
                    payload.getClass().getSimpleName().getBytes(StandardCharsets.UTF_8)));

            return kafkaTemplate.send(record).whenComplete((result, ex) -> {
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
            // Future 실패 상태로 반환
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }
}
