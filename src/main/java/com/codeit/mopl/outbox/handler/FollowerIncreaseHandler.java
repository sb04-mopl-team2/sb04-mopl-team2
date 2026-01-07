package com.codeit.mopl.outbox.handler;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.event.sender.KafkaEventSender;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowerIncreaseHandler implements FollowOutBoxHandler {

    private final KafkaEventSender sender;

    @Override
    public EventType supports() {
        return EventType.FOLLOWER_INCREASE;
    }

    @Override
    public void publish(FollowOutBoxEvent event) {
        try {
            log.info("kafka FollowerIncrease Event");
            FollowerIncreaseEvent followerIncreaseEvent = new FollowerIncreaseEvent(
                    event.getFollowId(),
                    event.getFolloweeId()
            );
            String key = event.getFolloweeId().toString();
            sender.send("mopl-follower-increase", key, followerIncreaseEvent);
            event.markPublished();
        } catch (Exception e) {
            if (event.getRetryCount() >= FollowOutBoxEvent.MAX_RETRY_COUNT) {
                event.markDead(e.getMessage());
            } else {
                event.markFailed(e.getMessage());
            }
        }
    }
}
