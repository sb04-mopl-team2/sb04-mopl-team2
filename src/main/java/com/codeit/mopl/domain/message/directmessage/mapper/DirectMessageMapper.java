package com.codeit.mopl.domain.message.directmessage.mapper;

import com.codeit.mopl.domain.base.FrontendKstOffsetAdjuster;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class, FrontendKstOffsetAdjuster.class})
public interface DirectMessageMapper {

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "adjustForFrontend")
    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(source = "sender", target = "sender")
    @Mapping(source = "receiver", target = "receiver")
    DirectMessageDto toDirectMessageDto(DirectMessage entity);
}
