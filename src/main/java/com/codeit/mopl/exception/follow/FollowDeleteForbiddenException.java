package com.codeit.mopl.exception.follow;

import java.util.Map;
import java.util.UUID;

public class FollowDeleteForbiddenException extends FollowException {
    public FollowDeleteForbiddenException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_DELETE_FORBIDDEN, details);
    }

    public static FollowDeleteForbiddenException withIds(UUID followId, UUID followerId, UUID requesterId) {
        Map<String, Object> details = Map.of(
                "followId", followId,
                "followerId", followerId,
                "requesterId", requesterId
        );
        return new FollowDeleteForbiddenException(details);
    }
}
