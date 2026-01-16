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

    /**
     * FAILED OutBox 배치 처리
     */
    @Transactional
    public void publishFailedEvents() {
        List<OutBoxEvent> events = outBoxEventRepository.findFailedTargets(PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty()) {
            log.info("[OutBox] FAILED 상태인 OutBox가 없습니다: events = {}", events);
            return;
        }
        log.info("[OutBox] FAILED 상태의 OutBox를 찾았습니다: totalCount = {}", events.size());
        for (OutBoxEvent event : events) {
            OutBoxHandler handler = getHandler(event);
            handler.publish(event);
        }
    }

    @Transactional
    public void publishEvent(OutBoxEvent event) {
        OutBoxHandler handler = getHandler(event);
        handler.publish(event);
    }

    private OutBoxHandler getHandler(OutBoxEvent event) {
        OutBoxHandler handler = handlers.get(event.getEventType());
        if (handler == null) {
            log.error("[OutBox] EventType에 대한 핸들러가 없습니다: eventType = {}, outBoxEventId = {}", event.getEventType(), event.getId());
        }
        return handler;
    }
}
