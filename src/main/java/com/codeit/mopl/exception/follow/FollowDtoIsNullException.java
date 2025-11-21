package com.codeit.mopl.exception.follow;

import java.util.Map;

public class FollowDtoIsNullException extends FollowException {
    public FollowDtoIsNullException(Map<String, Object> details) {
        super(FollowErrorCode.FOLLOW_DTO_IS_NULL, details);
    }

    public static FollowDtoIsNullException withDetails() {
        Map<String, Object> details = Map.of("followDto", "null");
        return new FollowDtoIsNullException(details);
    }
}
