package com.codeit.mopl.outbox.handler;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.sender.KafkaEventSender;
import com.codeit.mopl.exception.outbox.EventDeserializationFailedException;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.processor.OutBoxEventProcessor;
import com.codeit.mopl.outbox.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowerDecreaseHandler implements OutBoxHandler {

    private final KafkaEventSender sender;
    private final EventSerializer serializer;
    private final OutBoxEventProcessor processor;

    @Override
    public EventType supports() {
        return EventType.FOLLOWER_DECREASE;
    }

    @Override
    public void publish(OutBoxEvent event) {
        try {
            log.info("kafka FollowerDecrease Event");
            if (event.getOutBoxStatus() == OutBoxStatus.PUBLISHED) {
                log.info("[OutBox] 해당 OutBox는 이미 PUBLISHED 되었으므로 이벤트 발행을 중단합니다: outBoxEventId = {}", event.getId());
                return;
            }

            FollowerDecreaseEvent followerDecreaseEvent = serializer.deserialize(event, FollowerDecreaseEvent.class);
            String key = followerDecreaseEvent.followeeId().toString();

            sender.send("mopl-follower-decrease", key, followerDecreaseEvent)
                    .whenComplete((result, ex) -> processor.processKafkaResult(event, ex));
        } catch (EventDeserializationFailedException e) {
            // 구조적인 문제 -> 재시도 X
            processor.handleDeserializationFailure(event, e.getMessage());
        }
    }
}
