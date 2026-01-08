package com.codeit.mopl.outbox.handler;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.OutBoxEvent;

public interface OutBoxHandler {
    EventType supports();

    void publish(OutBoxEvent event);
}
