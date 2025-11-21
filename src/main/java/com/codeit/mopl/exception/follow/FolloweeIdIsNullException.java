package com.codeit.mopl.exception.follow;

import java.util.Map;

public class FolloweeIdIsNullException extends FollowException {
    public FolloweeIdIsNullException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOWEE_ID_IS_NULL, details);
    }

    public static FolloweeIdIsNullException withClassName(String className) {
        Map<String, Object> details = Map.of("className", className);
        return new FolloweeIdIsNullException(details);
    }
}
