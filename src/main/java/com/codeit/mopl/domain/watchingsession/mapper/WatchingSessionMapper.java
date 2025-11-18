package com.codeit.mopl.domain.watchingsession.mapper;

import com.codeit.mopl.domain.content.dto.response.contentSummary;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WatchingSessionMapper {
  @Mapping(source = "user", target = "userSummary")
  @Mapping(source = "content", target = "contentSummary")
  WatchingSessionDto toDto(WatchingSession watchingSession);

  UserSummary userToUserSummary(User user);

  @Mapping(source = "contentType.type", target = "type")
  contentSummary contentToContentSummary(Content content);
}
