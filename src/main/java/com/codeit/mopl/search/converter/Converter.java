package com.codeit.mopl.search.converter;

import com.codeit.mopl.search.document.AbstractDocument;
import java.time.LocalDateTime;

// E - Document 클래스, T - DTO
public interface Converter<E extends AbstractDocument, T> {

  Class<E> getDocumentClass();

  T convertToDto(E document);

  E convertToDocument(T dto, LocalDateTime createdAt);
}