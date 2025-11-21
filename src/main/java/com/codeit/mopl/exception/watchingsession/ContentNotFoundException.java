package com.codeit.mopl.exception.watchingsession;

import java.util.Map;

public class ContentNotFoundException extends WatchingSessionException {
  public ContentNotFoundException(WatchingSessionErrorCode watchingSessionErrorCode, Map<String, Object> details) {
    super(watchingSessionErrorCode,details);
  }
}
