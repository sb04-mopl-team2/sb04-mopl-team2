package com.codeit.mopl.exception.watchingsession;

import com.codeit.mopl.exception.global.MoplException;
import com.codeit.mopl.exception.watchingsession.ErrorCode;
import java.util.Map;

public class WatchingSessionException extends MoplException {
  public WatchingSessionException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}