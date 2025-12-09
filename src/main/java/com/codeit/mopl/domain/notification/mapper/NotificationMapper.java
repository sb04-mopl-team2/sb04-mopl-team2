package com.codeit.mopl.domain.notification.mapper;

import com.codeit.mopl.domain.base.TimeUtil;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = TimeUtil.class)
public interface NotificationMapper {
  @Mapping(target = "createdAt", expression = "java(TimeUtil.toKst(entity.getCreatedAt()))")
  @Mapping(target = "receiverId", source = "user.id")
  NotificationDto toDto(Notification entity);
}
