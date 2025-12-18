package com.codeit.mopl.exception.content;

import java.util.Map;

public class ContentDocumentNotFoundException extends ContentException {

  public ContentDocumentNotFoundException(ContentErrorCode contentErrorCode, Map<String, Object> details) {
    super(contentErrorCode, details);
  }
}
