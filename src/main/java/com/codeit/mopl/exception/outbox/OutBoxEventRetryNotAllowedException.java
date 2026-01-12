package com.codeit.mopl.exception.outbox;

import com.codeit.mopl.outbox.entity.OutBoxStatus;

import java.util.Map;
import java.util.UUID;

public class OutBoxEventRetryNotAllowedException extends OutBoxException {
    public OutBoxEventRetryNotAllowedException(Map<String, Object> details) {
        super(OutBoxErrorCode.OUTBOX_EVENT_RETRY_NOT_ALLOWED, details);
    }

    public static OutBoxEventRetryNotAllowedException withIdAndStatus(UUID outBoxId, OutBoxStatus outBoxStatus) {
        Map<String, Object> details = Map.of("outBoxId", outBoxId, "OutBoxStatus", outBoxStatus.name());
        return new OutBoxEventRetryNotAllowedException(details);
    }
}
