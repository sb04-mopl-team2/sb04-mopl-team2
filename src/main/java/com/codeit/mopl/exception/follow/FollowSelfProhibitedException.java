package com.codeit.mopl.exception.follow;

import java.util.Map;
import java.util.UUID;

public class FollowSelfProhibitedException extends FollowException {
    public FollowSelfProhibitedException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_SELF_PROHIBITED, details);
    }

    public static FollowSelfProhibitedException withFollowerIdAndFolloweeId(UUID followerId, UUID followeeId) {
        Map<String, Object> details = Map.of("followerId", followerId, "followeeId", followeeId);
        return new FollowSelfProhibitedException(details);
    }
}
