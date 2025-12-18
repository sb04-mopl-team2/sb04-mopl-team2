package com.codeit.mopl.event.event;

import java.util.UUID;

public record UserLogInOutEvent(
        UUID userId,
        boolean status
) {
}
