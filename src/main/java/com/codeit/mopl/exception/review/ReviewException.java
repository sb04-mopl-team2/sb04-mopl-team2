package com.codeit.mopl.exception.review;

import com.codeit.mopl.exception.global.MoplException;
import java.util.Map;

public class ReviewException extends MoplException {
    public ReviewException(ReviewErrorCode reviewErrorCode, Map<String, Object> details) {
        super(reviewErrorCode, details);
    }
}
