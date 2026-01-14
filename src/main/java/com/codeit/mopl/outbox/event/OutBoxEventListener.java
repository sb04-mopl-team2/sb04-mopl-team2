package com.codeit.mopl.outbox.event;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.outbox.dto.OutBoxEventCreateRequest;
import com.codeit.mopl.outbox.service.OutBoxEventService;
import com.codeit.mopl.outbox.entity.AggregateType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutBoxEventListener {

    private final OutBoxEventService outBoxEventService;

    @EventListener
    public void handleFollowerIncreaseEvent(FollowerIncreaseEvent event) {
        OutBoxEventCreateRequest request = createRequest(EventType.FOLLOWER_INCREASE, AggregateType.FOLLOW, event.followId(), event);
        outBoxEventService.createOutBoxEvent(request);
    }

    @EventListener
    public void handleFollowerDecreaseEvent(FollowerDecreaseEvent event) {
        OutBoxEventCreateRequest request = createRequest(EventType.FOLLOWER_INCREASE, AggregateType.FOLLOW, event.followId(), event);
        outBoxEventService.createOutBoxEvent(request);
    }

    private OutBoxEventCreateRequest createRequest(EventType eventType, AggregateType aggregateType, UUID aggregateId, Object domainEvent) {
        return new OutBoxEventCreateRequest(
                eventType,
                aggregateType,
                aggregateId,
                domainEvent
        );
    }
}
