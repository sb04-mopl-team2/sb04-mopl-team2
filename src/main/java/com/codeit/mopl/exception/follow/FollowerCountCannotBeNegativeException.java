package com.codeit.mopl.exception.follow;

import java.util.Map;
import java.util.UUID;

public class FollowerCountCannotBeNegativeException extends FollowException {
    public FollowerCountCannotBeNegativeException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOWER_COUNT_CANNOT_BE_NEGATIVE, details);
    }

    public static FollowerCountCannotBeNegativeException withFolloweeIdAndFollowerCount(UUID followeeId, long followerCount) {
        Map<String, Object> details = Map.of("followeeId", followeeId, "followerCount", followerCount);
        return new FollowerCountCannotBeNegativeException(details);
    }
}
