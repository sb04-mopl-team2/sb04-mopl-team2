package com.codeit.mopl.search.converter;

import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.search.document.ContentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentDocumentMapper {

  @Mapping(target = "contentType", source = "type")
  Content toContent(ContentDocument document);

  @Mapping(target = "type", source = "contentType")
  ContentDocument toDocument(Content content);

  ContentDocument fromDtoToDocument(ContentDto dto);

  @Mapping(target = "watcherCount", source = "watcherCount")
  ContentDto fromDocumentToDto(ContentDocument doc, Long watcherCount);

  default ContentType stringToEnum(String type) {
    if (type == null) return null;
    return ContentType.fromType(type);
  }

  default String enumToString(ContentType type) {
    if (type == null) return null;
    return type.getType(); // Or type.name()
  }
}
