package com.codeit.mopl.domain.watchingsession.mapper;

import com.codeit.mopl.domain.content.dto.response.ContentSummary;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WatchingSessionMapper {
  @Mapping(source = "user", target = "watcher")
  WatchingSessionDto toDto(WatchingSession watchingSession);

  @Mapping(target = "userId", source = "id")
  UserSummary userToUserSummary(User user);

  @Mapping(source = "contentType.type", target = "type")
  ContentSummary contentToContentSummary(Content content);
}
