package com.codeit.mopl.domain.message.directmessage.dto;

import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.user.dto.response.UserSummary;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    String createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {
    public static DirectMessageDto from(DirectMessage dm) {
        return new DirectMessageDto(
                dm.getId(),
                dm.getConversation().getId(),
                dm.getCreatedAt()
                        .atZone(ZoneId.of("Asua/Seoul"))
                        .toLocalDateTime()
                        .toString(),
                new UserSummary(
                        dm.getSender().getId(),
                        dm.getSender().getName(),
                        dm.getSender().getProfileImageUrl()
                ),
                new UserSummary(
                        dm.getReceiver().getId(),
                        dm.getReceiver().getName(),
                        dm.getReceiver().getProfileImageUrl()
                ),
                dm.getContent()
        );
    }
}
