package com.codeit.mopl.outbox.handler;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.event.sender.KafkaEventSender;
import com.codeit.mopl.exception.outbox.EventDeserializationFailedException;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.util.ErrorMessageSummarizer;
import com.codeit.mopl.outbox.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowerIncreaseHandler implements OutBoxHandler {

    private final KafkaEventSender sender;
    private final EventSerializer serializer;

    @Override
    public EventType supports() {
        return EventType.FOLLOWER_INCREASE;
    }

    @Override
    public void publish(OutBoxEvent event) {
        try {
            log.info("kafka FollowerIncrease Event");
            FollowerIncreaseEvent followerIncreaseEvent = serializer.deserialize(event, FollowerIncreaseEvent.class);
            String key = followerIncreaseEvent.followeeId().toString();
            sender.send("mopl-follower-increase", key, followerIncreaseEvent);
            event.markPublished();

        } catch (EventDeserializationFailedException e) {
            // 구조적인 문제 -> 재시도 X
            log.error("[OutBox] payload 역직렬화 실패 -> DEAD: event = {}, errorMessage = {}", event, e.getMessage(), e);
            event.markDead(e.getMessage());

        } catch (Exception e) {
            String errorMessage = ErrorMessageSummarizer.summarizeErrorMessage(e.getMessage());
            if (event.getRetryCount() >= OutBoxEvent.MAX_RETRY_COUNT) {
                event.markDead(errorMessage);
            } else {
                event.markFailed(errorMessage);
            }
        }
    }
}
