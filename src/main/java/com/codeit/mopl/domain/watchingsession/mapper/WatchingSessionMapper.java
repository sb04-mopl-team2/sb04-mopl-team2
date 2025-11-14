package com.codeit.mopl.domain.watchingsession.mapper;

import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WatchingSessionMapper {

  default WatchingSessionDto toDto(WatchingSession watchingSession) {
    User user = watchingSession.getUser();

    return new WatchingSessionDto(
        watchingSession.getId(),
        watchingSession.getCreatedAt(),
        new UserSummary(
            user.getId(),
            user.getEmail(),
            user.getProfileImageUrl()
        )
        // add contentsummary
    );
  }

}
