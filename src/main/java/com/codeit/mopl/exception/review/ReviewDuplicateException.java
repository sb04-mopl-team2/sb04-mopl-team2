package com.codeit.mopl.exception.review;

import java.util.Map;

public class ReviewDuplicateException extends ReviewException {

    public ReviewDuplicateException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode,details);
    }
}
