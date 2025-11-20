package com.codeit.mopl.exception.follow;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class FollowException extends MoplException {
    public FollowException(ErrorCodeInterface errorCode, Map<String, Object> details) {
      super(errorCode, details);
    }
}
