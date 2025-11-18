package com.codeit.mopl.exception.playlist;

import com.codeit.mopl.exception.global.ErrorCode;
import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class PlaylistNotFoundException extends MoplException {
    public PlaylistNotFoundException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
