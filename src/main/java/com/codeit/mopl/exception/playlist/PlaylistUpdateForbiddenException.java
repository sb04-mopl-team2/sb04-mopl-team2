package com.codeit.mopl.exception.playlist;


import java.util.Map;
import java.util.UUID;

public class PlaylistUpdateForbiddenException extends PlaylistException {
    public PlaylistUpdateForbiddenException(UUID playlistId) {
        super(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN,
                Map.of("playlistId", playlistId.toString()));
    }
}
