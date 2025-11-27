package com.codeit.mopl.domain.message.conversation.dto.response;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.user.dto.response.UserSummary;

import java.util.UUID;

public record ConversationDto (
        UUID id,
        UserSummary with,
        DirectMessageDto lastestMessage,
        boolean hasUnread
) { }
