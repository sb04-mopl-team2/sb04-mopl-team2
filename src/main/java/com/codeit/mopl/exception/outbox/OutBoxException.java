package com.codeit.mopl.exception.outbox;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class OutBoxException extends MoplException {
    public OutBoxException(ErrorCodeInterface errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
