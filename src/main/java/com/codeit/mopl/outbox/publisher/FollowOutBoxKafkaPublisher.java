package com.codeit.mopl.outbox.publisher;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import com.codeit.mopl.outbox.handler.FollowOutBoxHandler;
import com.codeit.mopl.outbox.repository.FollowOutBoxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FollowOutBoxKafkaPublisher {

    private static final int BATCH_SIZE = 100;

    private final FollowOutBoxEventRepository followOutBoxEventRepository;
    private final Map<EventType, FollowOutBoxHandler> handlers;

    public FollowOutBoxKafkaPublisher(FollowOutBoxEventRepository repository, List<FollowOutBoxHandler> handlerList) {
        this.followOutBoxEventRepository = repository;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        FollowOutBoxHandler::supports,
                        Function.identity()
                ));
    }

    @Transactional
    public void publish() {
        List<FollowOutBoxEvent> events = followOutBoxEventRepository.findPublishTargets(PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty()) {
            log.info("[팔로우 관리] REQUESTED 혹은 FAILED 상태인 FollowOutBoxEvent 객체가 없습니다: events = {}", events);
            return;
        }
        for (FollowOutBoxEvent event : events) {
            handlers.get(event.getEventType()).publish(event);
        }
    }
}
