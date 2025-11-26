package com.codeit.mopl.event.consumer;

import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowEventKafkaConsumer {

    private final FollowService followService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "mopl-follower-increase", groupId = "mopl-follow", concurrency = "3")
    public void onFollowIncrease(String kafkaEventJson, Acknowledgment ack) {
        try {
            FollowerIncreaseEvent event = objectMapper.readValue(kafkaEventJson, FollowerIncreaseEvent.class);
            UUID followId = event.followId();
            UUID followeeId = event.followeeId();

            // 이미 처리된 이벤트인지 검증
            ProcessedEvent processedEvent = new ProcessedEvent(followId, EventType.FOLLOWER_INCREASE);
            try {
                processedEventRepository.save(processedEvent);
            } catch (DataIntegrityViolationException e) {
                printAlreadyProcessedWarnLogMessage(processedEvent);
                ack.acknowledge();
                return;
            }
            followService.increaseFollowerCount(followeeId);
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[Kafka] 팔로워 증가 이벤트 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] 팔로워 증가 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }

    @KafkaListener(topics = "mopl-follower-decrease", groupId = "mopl-follow", concurrency = "3")
    public void onFollowDecrease(String kafkaEventJson, Acknowledgment ack) {
        try {
            FollowerDecreaseEvent event = objectMapper.readValue(kafkaEventJson, FollowerDecreaseEvent.class);
            UUID followId = event.followId();
            UUID followeeId = event.followeeId();

            // 이미 처리된 이벤트인지 검증
            ProcessedEvent processedEvent = new ProcessedEvent(followId, EventType.FOLLOWER_DECREASE);
            try {
                processedEventRepository.save(processedEvent);
            } catch (DataIntegrityViolationException e) {
                printAlreadyProcessedWarnLogMessage(processedEvent);
                ack.acknowledge();
                return;
            }
            followService.decreaseFollowerCount(followeeId);
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[Kafka] 팔로워 감소 이벤트 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] 팔로워 감소 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }

    private void printAlreadyProcessedWarnLogMessage(ProcessedEvent processedEvent) {
        UUID eventId = processedEvent.getEventId();
        EventType eventType = processedEvent.getEventType();
        log.warn("[Kafka] 이미 처리된 이벤트입니다: eventId = {}, eventType = {}", eventId, eventType);
    }
}
