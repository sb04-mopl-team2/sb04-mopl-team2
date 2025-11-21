package com.codeit.mopl.exception.playlist;

import java.util.HashMap;
import java.util.UUID;

public class PlaylistItemNotFoundException extends PlaylistException {
    private PlaylistItemNotFoundException() { super(PlaylistErrorCode.PLAYLISTITEM_NOT_FOUND, new HashMap<>());}

    public static PlaylistItemNotFoundException withId(UUID contentId) {
        PlaylistItemNotFoundException ex = new PlaylistItemNotFoundException();
        ex.getDetails().put("contentId", contentId);
        return ex;
    }
}
