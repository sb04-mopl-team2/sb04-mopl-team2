package com.codeit.mopl.exception.follow;

import com.codeit.mopl.domain.follow.entity.FollowStatus;

import java.util.Map;
import java.util.UUID;

public class FollowCannotDeleteWhileProcessingException extends FollowException {
    public FollowCannotDeleteWhileProcessingException(Map<String ,Object> details) {
        super(FollowErrorCode.FOLLOW_CANNOT_DELETE_WHILE_PROCESSING, details);
    }

    public static FollowCannotDeleteWhileProcessingException withIdAndStatus(UUID followId, FollowStatus followStatus) {
        Map<String, Object> details = Map.of(
                "followId", followId,
                "followStatus", followStatus
        );
        return new FollowCannotDeleteWhileProcessingException(details);
    }
}
