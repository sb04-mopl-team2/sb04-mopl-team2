package com.codeit.mopl.exception.follow;

import java.util.Map;

public class FollowIdIsNullException extends FollowException {
    public FollowIdIsNullException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_ID_IS_NULL, details);
    }

    public static FollowIdIsNullException withDetails() {
        Map<String, Object> details = Map.of("followId", "null");
        return new FollowIdIsNullException(details);
    }
}
