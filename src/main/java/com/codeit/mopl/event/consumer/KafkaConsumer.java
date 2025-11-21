package com.codeit.mopl.event.consumer;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @KafkaListener(topics = "mopl-notification-create", groupId = "mopl-notification", concurrency = "3")
    public void onNotificationCreated(String kafkaEventJson, Acknowledgment ack) {
        try {
            NotificationCreateEvent event = objectMapper.readValue(kafkaEventJson, NotificationCreateEvent.class);
            NotificationDto notificationDto = event.notificationDto();

            try {
                processedEventRepository.save(new ProcessedEvent(notificationDto.id(), EventType.NOTIFICATION_CREATED));
            } catch (DataIntegrityViolationException e) {
                log.warn("[Kafka] 이미 처리된 알림 생성 이벤트입니다. eventId={}", notificationDto.id());
                ack.acknowledge();
                return;
            }

            notificationService.sendNotification(notificationDto);
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[Kafka] 알림 생성 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] 알림 생성 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }
}
