package com.codeit.mopl.exception.outbox;

import java.util.HashMap;
import java.util.Map;

public class EventSerializationFailedException extends OutBoxException {
    public EventSerializationFailedException(Map<String, Object> details) {
        super(OutBoxErrorCode.EVENT_SERIALIZATION_FAILED, details);
    }

    public static EventSerializationFailedException withDetails(Object event) {
        Map<String, Object> details = new HashMap<>();
        if (event == null) {
            details.put("eventClass", "null");
            return new EventSerializationFailedException(details);
        }
        details.put("eventClass", event.getClass().getSimpleName());
        return new EventSerializationFailedException(details);
    }
}
