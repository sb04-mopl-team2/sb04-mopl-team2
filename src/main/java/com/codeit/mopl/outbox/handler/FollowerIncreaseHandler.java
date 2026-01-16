package com.codeit.mopl.outbox.handler;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.event.sender.KafkaEventSender;
import com.codeit.mopl.exception.outbox.EventDeserializationFailedException;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
import com.codeit.mopl.outbox.util.ErrorMessageSummarizer;
import com.codeit.mopl.outbox.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowerIncreaseHandler implements OutBoxHandler {

    private final KafkaEventSender sender;
    private final EventSerializer serializer;
    private final OutBoxEventRepository outBoxEventRepository;

    @Override
    public EventType supports() {
        return EventType.FOLLOWER_INCREASE;
    }

    @Override
    @Transactional
    public void publish(OutBoxEvent event) {
        try {
            log.info("kafka FollowerIncrease Event");
            if (event.getOutBoxStatus() == OutBoxStatus.PUBLISHED) {
                log.info("[OutBox] 해당 OutBox는 이미 PUBLISHED 되었으므로 이벤트 발행을 중단합니다: outBoxEventId = {}", event.getId());
                return;
            }

            FollowerIncreaseEvent followerIncreaseEvent = serializer.deserialize(event, FollowerIncreaseEvent.class);
            String key = followerIncreaseEvent.followeeId().toString();

            sender.send("mopl-follower-increase", key, followerIncreaseEvent)
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    String errorMessage = ErrorMessageSummarizer.summarizeErrorMessage(ex.getMessage());
                                    if (event.getRetryCount() == OutBoxEvent.MAX_RETRY_COUNT) {
                                        event.markDead(errorMessage);
                                    }
                                    event.markFailed(errorMessage);
                                } else {
                                    event.markPublished();
                                }
                                outBoxEventRepository.save(event);
                            });
        } catch (EventDeserializationFailedException e) {
            // 구조적인 문제 -> 재시도 X
            log.error("[OutBox] payload 역직렬화 실패 -> DEAD: event = {}, errorMessage = {}", event, e.getMessage(), e);
            event.markDead(e.getMessage());
            outBoxEventRepository.save(event);
        }
    }
}
