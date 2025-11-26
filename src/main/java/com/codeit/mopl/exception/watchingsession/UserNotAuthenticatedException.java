package com.codeit.mopl.exception.watchingsession;

import java.util.Map;

public class UserNotAuthenticatedException extends WatchingSessionException {
  public UserNotAuthenticatedException(WatchingSessionErrorCode watchingSessionErrorCode, Map<String, Object> details) {
    super(watchingSessionErrorCode,details);
  }
}
