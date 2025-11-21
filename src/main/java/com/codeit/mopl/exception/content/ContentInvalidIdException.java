package com.codeit.mopl.exception.content;

import java.util.Map;

public class ContentInvalidIdException extends ContentException {

  public ContentInvalidIdException(ContentErrorCode contentErrorCode, Map<String, Object> details) {
    super(contentErrorCode, details);
  }
}