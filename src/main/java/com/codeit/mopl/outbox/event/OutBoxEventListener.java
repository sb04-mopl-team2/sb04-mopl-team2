package com.codeit.mopl.outbox.event;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.outbox.service.OutBoxEventService;
import com.codeit.mopl.outbox.util.AggregateTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutBoxEventListener {

    private final OutBoxEventService outBoxEventService;

    @EventListener
    public void handleFollowerIncreaseEvent(FollowerIncreaseEvent event) {
        outBoxEventService.createOutBoxEvent(EventType.FOLLOWER_INCREASE, AggregateTypes.FOLLOW, event.followId(), event);
    }

    @EventListener
    public void handleFollowerDecreaseEvent(FollowerDecreaseEvent event) {
        outBoxEventService.createOutBoxEvent(EventType.FOLLOWER_DECREASE, AggregateTypes.FOLLOW, event.followId(), event);
    }
}
