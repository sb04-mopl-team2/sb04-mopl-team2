package com.codeit.mopl.event.event;

import java.util.UUID;

public record MailSendEvent(
        UUID eventId,
        String email,
        String tempPw
) {
}
