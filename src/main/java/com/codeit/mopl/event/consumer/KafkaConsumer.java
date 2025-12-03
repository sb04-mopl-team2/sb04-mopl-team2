package com.codeit.mopl.event.consumer;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.DirectMessageCreateEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.event.UserLogInOutEvent;
import com.codeit.mopl.event.event.UserRoleUpdateEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.codeit.mopl.sse.service.SseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SseService sseService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Transactional
    @KafkaListener(topics = "mopl-notification-create", groupId = "mopl-notification", concurrency = "3")
    public void onNotificationCreated(String kafkaEventJson, Acknowledgment ack) {
        try {
            NotificationCreateEvent event = objectMapper.readValue(kafkaEventJson, NotificationCreateEvent.class);
            NotificationDto notificationDto = event.notificationDto();

            Optional<ProcessedEvent> processedEvent = processedEventRepository.findByEventIdAndEventType(notificationDto.id(), EventType.NOTIFICATION_CREATED);
            if (processedEvent.isPresent()) {
                log.warn("[Kafka] 이미 처리된 알림 생성 이벤트입니다. eventId = {}", processedEvent.get().getId());
                ack.acknowledge();
                return;
            }

            notificationService.sendNotification(notificationDto);
            processedEventRepository.save(new ProcessedEvent(notificationDto.id(), EventType.NOTIFICATION_CREATED));
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[Kafka] 알림 생성 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] 알림 생성 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }

    @Transactional
    @KafkaListener(topics = {"mopl-user-role-update"}, groupId = "mopl-notification", concurrency = "3")
    public void onNotificationCreate(String kafkaEventJson, Acknowledgment ack) {
        try {
            UserRoleUpdateEvent event = objectMapper.readValue(kafkaEventJson, UserRoleUpdateEvent.class);
            UUID userId = event.userId();
            Optional<ProcessedEvent> processedEvent = processedEventRepository.findByEventIdAndEventType(event.eventId(), EventType.NOTIFICATION_CREATE);
            if (processedEvent.isPresent()) {
                log.warn("[Kafka] 이미 처리된 알림 생성 이벤트입니다. eventId = {}", processedEvent.get().getId());
                ack.acknowledge();
                return;
            }
            String title = "내 권한이 변경되었어요.";
            String content = String.format("내 권한이 [%s]에서 [%s]로 변경되었어요.",event.beforeRole(), event.afterRole());
            notificationService.createNotification(userId, title, content, Level.INFO);
            processedEventRepository.save(new ProcessedEvent(event.eventId(), EventType.NOTIFICATION_CREATE));
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[Kafka] 알림 생성 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] 알림 생성 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }

    @KafkaListener(topics = "mopl-user-login-out", groupId = "mopl-login-out", concurrency = "3")
    public void onUserLogInOutEvent(String kafkaEvent, Acknowledgment ack) {
        try {
            log.info("[Kafka] User LogInOut 이벤트");
            UserLogInOutEvent event = objectMapper.readValue(kafkaEvent,
                    UserLogInOutEvent.class);
            if (event.status()) {
                log.info("[Kafka] 유저 로그인 SseConnect. userId = {}", event.userId());
                sseService.connect(event.userId(), null);
            } else {
                log.info("[Kafka] 유저 로그아웃 SseEmitter 제거 userId = {}", event.userId());
                sseEmitterRegistry.getData().remove(event.userId());
            }
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[Kafka] LogInOut 이벤트 역직렬화 실패: {}", kafkaEvent, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] LogInOut 이벤트 처리 실패: {}", kafkaEvent, e);
            throw e;
        }
    }

    @Transactional
    @KafkaListener(topics = "mopl-directMessage-create", groupId = "mopl-notification", concurrency = "3")
    public void onDirectMessageCreated(String kafkaEventJson, Acknowledgment ack) {
        try {
            DirectMessageCreateEvent event = objectMapper.readValue(kafkaEventJson, DirectMessageCreateEvent.class);
            DirectMessageDto directMessageDto = event.directMessageDto();

            Optional<ProcessedEvent> processedEvent = processedEventRepository.findByEventIdAndEventType(directMessageDto.id(), EventType.DIRECT_MESSAGE_CREATED);
            if (processedEvent.isPresent()) {
                log.warn("[Kafka] 이미 처리된 DM 생성 이벤트입니다. eventId = {}", processedEvent.get().getId());
                ack.acknowledge();
                return;
            }

            notificationService.sendDirectMessage(directMessageDto);
            processedEventRepository.save(new ProcessedEvent(directMessageDto.id(), EventType.DIRECT_MESSAGE_CREATED));
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[Kafka] DM 생성 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] DM 생성 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }

}
