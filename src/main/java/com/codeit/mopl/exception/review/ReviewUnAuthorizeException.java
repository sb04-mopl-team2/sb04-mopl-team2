package com.codeit.mopl.exception.review;

import java.util.Map;

public class ReviewUnAuthorizeException extends ReviewException {

    public ReviewUnAuthorizeException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode,details);
    }
}