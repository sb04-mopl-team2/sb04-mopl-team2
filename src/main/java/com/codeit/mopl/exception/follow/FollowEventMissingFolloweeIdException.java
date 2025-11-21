package com.codeit.mopl.exception.follow;

import java.util.Map;

public class FollowEventMissingFolloweeIdException extends FollowException {
    public FollowEventMissingFolloweeIdException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_EVENT_MISSING_FOLLOWEE_ID, details);
    }
}
