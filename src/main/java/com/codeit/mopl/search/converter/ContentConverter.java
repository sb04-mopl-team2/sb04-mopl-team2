package com.codeit.mopl.search.converter;

import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.search.document.ContentDocument;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentConverter implements Converter<ContentDocument, ContentDto> {

  private final ContentDocumentMapper mapper;

  @Override
  public Class<ContentDocument> getDocumentClass() {
    return ContentDocument.class;
  }

  @Override
  public ContentDto convertToDto(ContentDocument document) {
    if (document == null) return null;
    return mapper.toDto(document);
  }

  @Override
  public ContentDocument convertToDocument(ContentDto dto, Instant createdAt) {
    if (dto ==  null) return null;
    return mapper.toDocument(dto, createdAt);
  }
}
