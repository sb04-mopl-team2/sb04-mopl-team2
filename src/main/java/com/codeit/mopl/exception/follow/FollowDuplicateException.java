package com.codeit.mopl.exception.follow;

import java.util.Map;
import java.util.UUID;

public class FollowDuplicateException extends FollowException {
    public FollowDuplicateException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_DUPLICATE, details);
    }

    public static FollowDuplicateException withFollowerIdAndFolloweeId(UUID followerId, UUID followeeId) {
        Map<String, Object> details = Map.of("followerId", followerId, "followeeId", followeeId);
        return new FollowDuplicateException(details);
    }
}
