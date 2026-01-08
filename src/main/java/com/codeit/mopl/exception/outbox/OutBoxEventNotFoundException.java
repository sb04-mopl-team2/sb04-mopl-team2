package com.codeit.mopl.exception.outbox;

import java.util.Map;
import java.util.UUID;

public class OutBoxEventNotFoundException extends OutBoxException {
    public OutBoxEventNotFoundException(Map<String, Object> details) {
        super(OutBoxErrorCode.OUTBOX_EVENT_NOT_FOUND, details);
    }

    public static OutBoxEventNotFoundException withId(UUID outBoxEventId) {
        Map<String, Object> details = Map.of("outBoxEventId", outBoxEventId);
        return new OutBoxEventNotFoundException(details);
    }
}
