package com.codeit.mopl.exception.content;

import java.util.Map;

public class InvalidContentTypeException extends ContentException {

  public InvalidContentTypeException(Map<String, Object> details) {
    super(ContentErrorCode.INVALID_CONTENT_TYPE, details);
  }
}