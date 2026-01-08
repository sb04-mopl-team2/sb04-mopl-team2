package com.codeit.mopl.outbox.mapper;

import com.codeit.mopl.outbox.dto.OutBoxEventDto;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OutBoxEventMapper {
    OutBoxEventDto toDto(OutBoxEvent outBoxEvent);
}
