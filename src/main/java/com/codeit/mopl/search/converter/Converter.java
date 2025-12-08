package com.codeit.mopl.search.converter;

import com.codeit.mopl.search.document.AbstractDocument;
import java.time.LocalDateTime;

public interface Converter<E extends AbstractDocument, T> {

  Class<E> getDocumentClass();

  T convertToDto(E document);

  E convertToDocument(T dto, LocalDateTime createdAt);
}