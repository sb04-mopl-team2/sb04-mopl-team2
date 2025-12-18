package com.codeit.mopl.exception.global;

import com.codeit.mopl.exception.notification.NotificationErrorCode;
import java.util.Map;

public class InvalidInputValueException extends MoplException {
  public InvalidInputValueException(Map<String, Object> details) {
    super(ErrorCode.INVALID_INPUT_VALUE, details);
  }
}
