package com.codeit.mopl.domain.message.directmessage.dto;

import com.codeit.mopl.domain.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {
}
