package com.codeit.mopl.event.consumer;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowEventKafkaConsumer {

    private final FollowService followService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "mopl-follower-increase", groupId = "mopl-follow", concurrency = "3")
    public void onFollowIncrease(String kafkaEventJson, Acknowledgment ack) throws Exception {
        FollowerIncreaseEvent event = objectMapper.readValue(kafkaEventJson, FollowerIncreaseEvent.class);
        FollowDto followDto = event.followDto();
        followService.increaseFollowerCount(followDto);
        ack.acknowledge();
    }
}
