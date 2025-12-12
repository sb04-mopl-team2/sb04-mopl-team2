package com.codeit.mopl.exception.content;

import java.util.Map;

public class ContentOsStorageException extends ContentException {

  public ContentOsStorageException(ContentErrorCode contentErrorCode, Map<String, Object> details) {
    super(contentErrorCode, details);
  }
}
