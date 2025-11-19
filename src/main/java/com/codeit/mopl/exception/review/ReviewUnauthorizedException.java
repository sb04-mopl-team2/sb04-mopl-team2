package com.codeit.mopl.exception.review;

import java.util.Map;

public class ReviewUnauthorizedException extends ReviewException {

    public ReviewUnauthorizedException(ReviewErrorCode reviewErrorCode, Map<String, Object> details) {
        super(reviewErrorCode,details);
    }
}