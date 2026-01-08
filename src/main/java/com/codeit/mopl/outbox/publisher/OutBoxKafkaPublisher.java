package com.codeit.mopl.outbox.publisher;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.handler.OutBoxHandler;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
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
public class OutBoxKafkaPublisher {

    private static final int BATCH_SIZE = 100;

    private final OutBoxEventRepository outBoxEventRepository;
    private final Map<EventType, OutBoxHandler> handlers;

    public OutBoxKafkaPublisher(OutBoxEventRepository repository, List<OutBoxHandler> handlerList) {
        this.outBoxEventRepository = repository;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        OutBoxHandler::supports,
                        Function.identity()
                ));
    }

    @Transactional
    public void publish() {
        List<OutBoxEvent> events = outBoxEventRepository.findPublishTargets(PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty()) {
            log.info("[팔로우 관리] REQUESTED 혹은 FAILED 상태인 OutBox 이벤트 객체가 없습니다: events = {}", events);
            return;
        }
        for (OutBoxEvent event : events) {
            handlers.get(event.getEventType()).publish(event);
        }
    }
}
