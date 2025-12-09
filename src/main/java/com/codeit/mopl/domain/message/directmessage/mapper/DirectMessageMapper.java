package com.codeit.mopl.domain.message.directmessage.mapper;

import com.codeit.mopl.domain.base.TimeUtil;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class}, imports = TimeUtil.class)
public interface DirectMessageMapper {

    @Mapping(target = "createdAt", expression = "java(TimeUtil.toKst(entity.getCreatedAt()))")
    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(source = "sender", target = "sender")
    @Mapping(source = "receiver", target = "receiver")
    DirectMessageDto toDirectMessageDto(DirectMessage entity);
}
