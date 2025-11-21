package com.codeit.mopl.exception.playlist;

import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class PlaylistException extends MoplException {
    public PlaylistException(PlaylistErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
