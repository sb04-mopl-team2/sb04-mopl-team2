package com.codeit.mopl.domain.message.directmessage.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    LocalDateTime createdAt,
    UserSummaryDto sender,
    UserSummaryDto receiver,
    String content
) {
}
