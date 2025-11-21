package com.codeit.mopl.domain.follow.mapper;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FollowMapper {
    @Mapping(target = "followerId", source = "follower.id")
    @Mapping(target = "followeeId", source = "followee.id")
    FollowDto toDto(Follow follow);
}
