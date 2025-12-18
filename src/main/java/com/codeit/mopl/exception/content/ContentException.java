package com.codeit.mopl.exception.content;

import com.codeit.mopl.exception.global.MoplException;
import java.util.Map;

public class ContentException extends MoplException {
  public ContentException(ContentErrorCode contentErrorCode, Map<String, Object> details) {
    super(contentErrorCode, details);
  }
}