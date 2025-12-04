package com.codeit.mopl.event.event;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;

public record DirectMessageCreateEvent(
    DirectMessageDto directMessageDto
) {
}
