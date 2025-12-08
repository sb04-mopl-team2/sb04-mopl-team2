package com.codeit.mopl.search.converter;

import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.search.document.ContentDocument;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentDocumentMapper {

  ContentDto toDto(ContentDocument document);

  @Mapping(source = "createdAt", target = "createdAt")
  ContentDocument toDocument(ContentDto dto, LocalDateTime createdAt);
}
