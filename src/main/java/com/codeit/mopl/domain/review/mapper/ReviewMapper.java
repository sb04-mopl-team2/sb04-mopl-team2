package com.codeit.mopl.domain.review.mapper;

import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { UserMapper.class })
public interface ReviewMapper {

  @Mapping(source = "review.id", target = "id")
  @Mapping(source = "review.content.id", target = "contentId")
  @Mapping(source = "review.user", target = "author")   // User → UserSummary: UserMapper.toSummary() 자동 실행 됨
  @Mapping(source = "review.text", target = "text")
  @Mapping(source = "review.rating", target = "rating")
  ReviewDto toDto(Review review);
}