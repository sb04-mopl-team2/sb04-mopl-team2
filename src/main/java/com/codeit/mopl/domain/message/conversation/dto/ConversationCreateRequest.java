package com.codeit.mopl.domain.message.conversation.dto;

import java.util.UUID;

public record ConversationCreateRequest (
        UUID withUserId
)

{
}
