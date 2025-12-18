package com.codeit.mopl.exception.review;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReviewNotFoundException extends ReviewException {
    public ReviewNotFoundException(ReviewErrorCode reviewErrorCode, Map<String, Object> details) {
        super(reviewErrorCode,details);
    }
}
