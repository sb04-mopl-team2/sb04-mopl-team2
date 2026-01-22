package com.codeit.mopl.exception.outbox;

import java.util.HashMap;
import java.util.Map;

public class EventDeserializationFailedException extends OutBoxException {
    public EventDeserializationFailedException(Map<String, Object> details) {
        super(OutBoxErrorCode.EVENT_DESERIALIZATION_FAILED, details);
    }

    public static EventDeserializationFailedException withDetails(Object event) {
        Map<String, Object> details = new HashMap<>();
        if (event == null) {
            details.put("eventClass", "null");
            return new EventDeserializationFailedException(details);
        }
        details.put("eventClass", event.getClass().getSimpleName());
        return new EventDeserializationFailedException(details);
    }
}
