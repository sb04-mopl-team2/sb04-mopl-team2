package com.codeit.mopl.exception.outbox;

import java.util.Map;

public class EventDeserializationFailedException extends OutBoxException {
    public EventDeserializationFailedException(Map<String, Object> details) {
        super(OutBoxErrorCode.EVENT_DESERIALIZATION_FAILED, details);
    }

    public static EventDeserializationFailedException withDetails(Object event) {
        Map<String, Object> details = Map.of("eventClass", event.getClass().getSimpleName());
        return new EventDeserializationFailedException(details);
    }
}
