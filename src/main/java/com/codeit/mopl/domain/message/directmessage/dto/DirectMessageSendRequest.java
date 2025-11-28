package com.codeit.mopl.domain.message.directmessage.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record DirectMessageSendRequest(
        UUID conversationId,
        UUID receiverId,
        @NotBlank String content
) {
}
