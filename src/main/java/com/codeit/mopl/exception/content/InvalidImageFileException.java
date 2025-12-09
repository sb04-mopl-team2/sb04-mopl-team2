package com.codeit.mopl.exception.content;

import java.util.Map;

public class InvalidImageFileException extends ContentException {

  public InvalidImageFileException(ContentErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

