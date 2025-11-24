package com.codeit.mopl.exception.content;

import java.util.Map;

public class ContentNotFoundException extends ContentException {

  public ContentNotFoundException(ContentErrorCode contentErrorCode, Map<String, Object> details) {
    super(contentErrorCode, details);
  }
}