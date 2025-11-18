package com.codeit.mopl.exception.watchingsession;

import com.codeit.mopl.exception.watchingsession.ErrorCode;
import java.util.Map;

public class ContentNotFoundException extends WatchingSessionException {
  public ContentNotFoundException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode,details);
  }
}
