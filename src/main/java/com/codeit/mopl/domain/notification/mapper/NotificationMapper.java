package com.codeit.mopl.domain.notification.mapper;

import com.codeit.mopl.domain.base.FrontendKstOffsetAdjuster;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {FrontendKstOffsetAdjuster.class})
public interface NotificationMapper {

  @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "adjustForFrontend")
  @Mapping(target = "receiverId", source = "user.id")
  NotificationDto toDto(Notification entity);
}
