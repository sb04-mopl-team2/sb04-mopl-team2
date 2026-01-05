package com.codeit.mopl.outbox.handler;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;

public interface FollowOutBoxHandler {
    EventType supports();

    void publish(FollowOutBoxEvent event);
}
