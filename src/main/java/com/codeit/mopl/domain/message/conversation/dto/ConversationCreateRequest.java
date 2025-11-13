package com.codeit.mopl.domain.message.conversation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConversationCreateRequest (
        @NotNull
        UUID withUserId
)

{
}
