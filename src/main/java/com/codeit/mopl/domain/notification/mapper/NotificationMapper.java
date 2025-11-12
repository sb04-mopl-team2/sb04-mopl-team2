package com.codeit.mopl.domain.notification.mapper;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", imports = MapperUtils.class)
public interface NotificationMapper {
  @Mapping(target = "createdAt", expression = "java(MapperUtils.asInstant(entity.getCreatedAt()))")
  NotificationDto toDto(Notification entity);
}
