package com.codeit.mopl.domain.user.mapper;

import com.codeit.mopl.domain.base.TimeUtil;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.dto.response.UserSummary;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.s3.S3Storage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", imports = TimeUtil.class)
public abstract class UserMapper {
    @Autowired
    S3Storage s3Storage;

    @Mapping(target = "createdAt", expression = "java(TimeUtil.toKst(user.getCreatedAt()))")
    @Mapping(target = "profileImageUrl", expression = "java(user.getProfileImageUrl() != null ? s3Storage.getPresignedUrl(user.getProfileImageUrl()) : null)")
    public abstract UserDto toDto(User user);
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "profileImageUrl", expression = "java(user.getProfileImageUrl() != null ? s3Storage.getPresignedUrl(user.getProfileImageUrl()) : null)")
    public abstract UserSummary toSummary(User user);
}
