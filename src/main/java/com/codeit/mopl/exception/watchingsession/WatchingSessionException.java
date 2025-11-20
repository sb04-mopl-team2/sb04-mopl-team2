package com.codeit.mopl.exception.watchingsession;

import com.codeit.mopl.exception.global.MoplException;
import java.util.Map;

public class WatchingSessionException extends MoplException {
  public WatchingSessionException(WatchingSessionErrorCode watchingSessionErrorCode, Map<String, Object> details) {
    super(watchingSessionErrorCode, details);
  }
}