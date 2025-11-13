package com.codeit.mopl.domain.message.conversation.mapper;

import com.codeit.mopl.domain.message.conversation.dto.ConversationDto;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    //매핑 추가할 예정
    ConversationDto toConversationDto(Conversation entity);
}
