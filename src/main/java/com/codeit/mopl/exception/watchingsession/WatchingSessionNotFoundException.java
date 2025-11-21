package com.codeit.mopl.exception.watchingsession;

import java.util.Map;

public class WatchingSessionNotFoundException extends WatchingSessionException {

  public WatchingSessionNotFoundException(WatchingSessionErrorCode watchingSessionErrorCode, Map<String, Object> details) {
    super(watchingSessionErrorCode,details);
  }}
