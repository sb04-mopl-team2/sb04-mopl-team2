package com.codeit.mopl.domain.message.directmessage.mapper;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

    //매핑 추가할 예정
    DirectMessageDto toDirectMessageDto(DirectMessage entity);
}
