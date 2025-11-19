package com.codeit.mopl.domain.message.conversation.mapper;

import com.codeit.mopl.domain.message.conversation.dto.ConversationDto;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class})
public interface ConversationMapper {

    @Mapping(source ="entity.with", target = "with")
    @Mapping(source = "lastestMessage", target = "lastestMessage")
    ConversationDto toConversationDto(Conversation entity, DirectMessageDto lastestMessage);
}
