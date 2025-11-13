package com.codeit.mopl.domain.message.conversation.dto;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;

import java.util.UUID;

public record ConversationDto (
        UUID id,
        UserSummaryDto with,
        DirectMessageDto lastestMessage,
        boolean hasUnread
) { }
