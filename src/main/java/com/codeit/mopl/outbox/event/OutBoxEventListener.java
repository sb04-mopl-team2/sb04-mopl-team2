package com.codeit.mopl.outbox.event;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.outbox.dto.OutBoxEventCreateRequest;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.publisher.OutBoxKafkaPublisher;
import com.codeit.mopl.outbox.service.OutBoxEventService;
import com.codeit.mopl.outbox.entity.AggregateType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutBoxEventListener {

    private final OutBoxEventService outBoxEventService;
    private final OutBoxKafkaPublisher publisher;

    @EventListener
    public void handleFollowerIncreaseEvent(FollowerIncreaseEvent event) {
        OutBoxEventCreateRequest request = createRequest(EventType.FOLLOWER_INCREASE, AggregateType.FOLLOW, event.followId(), event);
        outBoxEventService.createOutBoxEvent(request);
    }

    @EventListener
    public void handleFollowerDecreaseEvent(FollowerDecreaseEvent event) {
        OutBoxEventCreateRequest request = createRequest(EventType.FOLLOWER_DECREASE, AggregateType.FOLLOW, event.followId(), event);
        outBoxEventService.createOutBoxEvent(request);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(OutBoxEvent event) {
        publisher.publishEvent(event);
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
