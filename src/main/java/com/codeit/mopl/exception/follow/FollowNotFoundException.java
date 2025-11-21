package com.codeit.mopl.exception.follow;

import java.util.Map;
import java.util.UUID;

public class FollowNotFoundException extends FollowException {
    public FollowNotFoundException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_NOT_FOUND, details);
    }

    public static FollowNotFoundException withId(UUID followId) {
      Map<String, Object> details = Map.of("followId", followId);
      return new FollowNotFoundException(details);
    }
}
