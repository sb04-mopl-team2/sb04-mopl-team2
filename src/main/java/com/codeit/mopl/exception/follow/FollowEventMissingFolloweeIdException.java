package com.codeit.mopl.exception.follow;

import java.util.Map;
import java.util.UUID;

public class FollowEventMissingFolloweeIdException extends FollowException {
    public FollowEventMissingFolloweeIdException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_EVENT_MISSING_FOLLOWEE_ID, details);
    }

    public static FollowEventMissingFolloweeIdException withId(UUID followeeId) {
      Map<String, Object> details = Map.of("followeeId", followeeId);
      return new FollowEventMissingFolloweeIdException(details);
    }
}
