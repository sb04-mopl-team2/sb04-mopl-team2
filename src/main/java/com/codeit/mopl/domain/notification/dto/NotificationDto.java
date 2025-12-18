package com.codeit.mopl.domain.notification.dto;

import com.codeit.mopl.domain.notification.entity.Level;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(

    @NotNull
    UUID id,

    @NotNull
    Instant createdAt,

    @NotNull
    UUID receiverId,

    @NotNull
    String title,

    @NotNull
    String content,

    @NotNull
    Level level

) {}
