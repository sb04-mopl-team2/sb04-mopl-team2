package com.codeit.mopl.exception.follow;

import java.util.Map;

public class FolloweeIdIsNullException extends FollowException {
    public FolloweeIdIsNullException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOWEE_ID_IS_NULL, details);
    }

    public static FolloweeIdIsNullException withDetails() {
        Map<String, Object> details = Map.of("followeeId", "null");
        return new FolloweeIdIsNullException(details);
    }
}
