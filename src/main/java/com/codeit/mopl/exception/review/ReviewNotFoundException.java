package com.codeit.mopl.exception.review;

import java.util.Map;

public class ReviewNotFoundException extends ReviewException {

    public ReviewNotFoundException(ReviewErrorCode reviewErrorCode, Map<String, Object> details) {
        super(reviewErrorCode,details);
    }
}
