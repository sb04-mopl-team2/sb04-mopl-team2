package com.codeit.mopl.exception.playlist;

import java.util.HashMap;
import java.util.UUID;

public class PlaylistNotFoundException extends PlaylistException {

    private PlaylistNotFoundException() {
        super(PlaylistErrorCode.PLAYLIST_NOT_FOUND, new HashMap<>());
    }

    public static PlaylistNotFoundException withId(UUID playlistId) {
        PlaylistNotFoundException exception = new PlaylistNotFoundException();
        exception.getDetails().put("playlistId", playlistId);
        return exception;
    }
}
