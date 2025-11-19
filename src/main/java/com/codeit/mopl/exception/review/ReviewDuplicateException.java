package com.codeit.mopl.exception.review;

import java.util.Map;

public class ReviewDuplicateException extends ReviewException {

    public ReviewDuplicateException(ReviewErrorCode reviewErrorCode, Map<String, Object> details) {
        super(reviewErrorCode,details);
    }
}
