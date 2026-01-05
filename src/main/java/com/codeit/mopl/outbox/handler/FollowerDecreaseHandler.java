package com.codeit.mopl.outbox.handler;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.sender.KafkaEventSender;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowerDecreaseHandler implements FollowOutBoxHandler {

    private final KafkaEventSender sender;

    @Override
    public EventType supports() {
        return EventType.FOLLOWER_DECREASE;
    }

    @Override
    public void publish(FollowOutBoxEvent event) {
        try {
            log.info("kafka FollowerDecrease Event");
            FollowerDecreaseEvent followerDecreaseEvent = new FollowerDecreaseEvent(
                    event.getFollowId(),
                    event.getFolloweeId()
            );
            String key = event.getFolloweeId().toString();
            sender.send("mopl-follower-decrease", key, followerDecreaseEvent);
            event.markPublished();
        } catch (Exception e) {
            event.markFailed();
        }
    }
}
