package com.codeit.mopl.exception.review;

import java.util.Map;

public class ReviewForbiddenException extends ReviewException {

    public ReviewForbiddenException(ReviewErrorCode reviewErrorCode, Map<String, Object> details) {
        super(reviewErrorCode,details);
    }
}