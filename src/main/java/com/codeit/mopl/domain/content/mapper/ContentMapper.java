package com.codeit.mopl.domain.content.mapper;

import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.request.ContentCreateRequest;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ContentMapper {

  @Mapping(target = "contentType", source = "request", qualifiedByName = "mappingContentType")
  @Mapping(target = "averageRating", constant = "0.0")
  @Mapping(target = "reviewCount", constant = "0")
  @Mapping(target = "thumbnailUrl", ignore = true)
  @Mapping(target = "watcherCount", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Content fromCreateRequest(ContentCreateRequest request);

  @Mapping(target = "type", expression = "java(content.getContentType().getType())")
  @Mapping(target = "watcherCount", source = "watcherCount")
  ContentDto toDto(Content content, Long watcherCount);

  @Named("mappingContentType")
  default ContentType mappingContentType(ContentCreateRequest request) {
    return ContentType.fromType(request.type());
  }
}
