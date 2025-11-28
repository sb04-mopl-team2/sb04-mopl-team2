package com.codeit.mopl.event.event;

import com.codeit.mopl.domain.user.entity.Role;

import java.util.UUID;

public record UserRoleUpdateEvent(
        UUID eventId,
        UUID userId,
        Role beforeRole,
        Role afterRole
) {
}
