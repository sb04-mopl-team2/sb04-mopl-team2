package com.codeit.mopl.event.consumer;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowEventKafkaConsumer {

    private final FollowService followService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "mopl-follower-increase", groupId = "mopl-follow", concurrency = "3")
    public void onFollowIncrease(String kafkaEventJson, Acknowledgment ack) throws Exception {
        try {
            FollowerIncreaseEvent event = objectMapper.readValue(kafkaEventJson, FollowerIncreaseEvent.class);
            FollowDto followDto = event.followDto();
            followService.increaseFollowerCount(followDto);
            ack.acknowledge();
        } catch (JsonParseException e) {
            log.error("[Kafka] 팔로워 증가 이벤트 역직렬화 실패: {}", kafkaEventJson, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Kafka] 팔로워 증가 이벤트 처리 실패: {}", kafkaEventJson, e);
            throw e;
        }
    }
}
