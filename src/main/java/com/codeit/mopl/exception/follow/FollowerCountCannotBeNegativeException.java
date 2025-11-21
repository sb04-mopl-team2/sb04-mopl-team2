package com.codeit.mopl.exception.follow;

import java.util.Map;
import java.util.UUID;

public class FollowerCountCannotBeNegativeException extends FollowException {
    public FollowerCountCannotBeNegativeException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOWER_COUNT_CANNOT_BE_NEGATIVE, details);
    }

    public static FollowerCountCannotBeNegativeException withId(UUID followId) {
        Map<String, Object> details = Map.of("followId", followId);
        return new FollowerCountCannotBeNegativeException(details);
    }
}
