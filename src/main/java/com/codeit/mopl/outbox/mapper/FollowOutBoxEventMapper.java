package com.codeit.mopl.outbox.mapper;

import com.codeit.mopl.outbox.dto.FollowOutBoxEventDto;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowOutBoxEventMapper {

    FollowOutBoxEventDto toDto(FollowOutBoxEvent followOutBoxEvent);
}
