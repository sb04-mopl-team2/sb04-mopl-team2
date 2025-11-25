package com.codeit.mopl.domain.message.conversation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConversationCreateRequest (
        @NotNull
        UUID withUserId
)

{
}
