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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowEventKafkaConsumer {

    private final FollowService followService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "mopl-follower-increase", groupId = "mopl-follow", concurrency = "3")
    @Transactional
    public void onFollowerIncrease(String kafkaEventJson, Acknowledgment ack) {
        try {
            FollowerIncreaseEvent event = objectMapper.readValue(kafkaEventJson, FollowerIncreaseEvent.class);
            UUID followId = event.followId();
            UUID followeeId = event.followeeId();

            // 이미 처리된 이벤트면 early return
            if (isAlreadyProcessed(followId, EventType.FOLLOWER_INCREASE)) {
                registerAfterCommitAck(ack);
                return;
            }

            followService.processFollowerIncrease(followId, followeeId);
            processedEventRepository.save(new ProcessedEvent(followId, EventType.FOLLOWER_INCREASE));
            registerAfterCommitAck(ack);

        } catch (JsonProcessingException e) {
            log.error("[Kafka] 팔로워 증가 이벤트 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();

        } catch (DataIntegrityViolationException e) {
            log.info("[Kafka] 이미 처리된 이벤트입니다: {}", kafkaEventJson, e);
            registerAfterCommitAck(ack);
        } catch (Exception e) {
            log.error("[Kafka] 팔로워 증가 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }

    @KafkaListener(topics = "mopl-follower-decrease", groupId = "mopl-follow", concurrency = "3")
    @Transactional
    public void onFollowerDecrease(String kafkaEventJson, Acknowledgment ack) {
        try {
            FollowerDecreaseEvent event = objectMapper.readValue(kafkaEventJson, FollowerDecreaseEvent.class);
            UUID followId = event.followId();
            UUID followeeId = event.followeeId();

            // 이미 처리된 이벤트면 early return
            if (isAlreadyProcessed(followId, EventType.FOLLOWER_DECREASE)) {
                registerAfterCommitAck(ack);
                return;
            }

            followService.processFollowerDecrease(followId, followeeId);
            processedEventRepository.save(new ProcessedEvent(followId, EventType.FOLLOWER_DECREASE));
            registerAfterCommitAck(ack);

        } catch (JsonProcessingException e) {
            log.error("[Kafka] 팔로워 감소 이벤트 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();

        } catch (DataIntegrityViolationException e) {
            log.info("[Kafka] 이미 처리된 이벤트입니다: {}", kafkaEventJson, e);
            registerAfterCommitAck(ack);
        } catch (Exception e) {
            log.error("[Kafka] 팔로워 감소 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }

    private boolean isAlreadyProcessed(UUID followId, EventType eventType) {
        boolean isProcessed = processedEventRepository.existsByEventIdAndEventType(followId, eventType);
        if (isProcessed) {
            log.warn("[Kafka] 이벤트 처리 중단 - 이미 처리된 이벤트입니다: eventId = {}, eventType = {}", followId, eventType);
        }
        return isProcessed;
    }

    private void registerAfterCommitAck(Acknowledgment ack) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ack.acknowledge();
                    }
                }
        );
    }
}
