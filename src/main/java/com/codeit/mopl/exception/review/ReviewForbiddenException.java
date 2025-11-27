package com.codeit.mopl.exception.review;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ReviewForbiddenException extends ReviewException {

    public ReviewForbiddenException(ReviewErrorCode reviewErrorCode, Map<String, Object> details) {
        super(reviewErrorCode,details);
    }
}