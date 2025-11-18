package com.codeit.mopl.exception.review;

import java.util.Map;

public class ReviewNotFoundException extends ReviewException {

    public ReviewNotFoundException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode,details);
    }
}
