package com.codeit.mopl.outbox.publisher;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import com.codeit.mopl.outbox.handler.FollowOutBoxHandler;
import com.codeit.mopl.outbox.repository.FollowOutBoxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FollowOutBoxKafkaPublisher {

    private final FollowOutBoxRepository followOutBoxRepository;
    private final Map<EventType, FollowOutBoxHandler> handlers;

    public FollowOutBoxKafkaPublisher(FollowOutBoxRepository repository, List<FollowOutBoxHandler> handlerList) {
        this.followOutBoxRepository = repository;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        FollowOutBoxHandler::supports,
                        Function.identity()
                ));
    }

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void publish() {
        List<FollowOutBoxEvent> events = followOutBoxRepository.findTop100ByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.PENDING);
        if (events.isEmpty()) {
            log.info("[팔로우 관리] PENDING 상태인 FollowOutBoxEvent 객체가 없습니다: events = {}", events);
            return;
        }
        for (FollowOutBoxEvent event : events) {
            handlers.get(event.getEventType()).publish(event);
        }
    }
}
