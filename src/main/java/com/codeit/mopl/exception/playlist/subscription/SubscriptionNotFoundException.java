package com.codeit.mopl.exception.playlist.subscription;

import com.codeit.mopl.exception.playlist.PlaylistErrorCode;
import com.codeit.mopl.exception.playlist.PlaylistException;

import java.util.HashMap;
import java.util.UUID;

public class SubscriptionNotFoundException extends PlaylistException {
    private SubscriptionNotFoundException() { super(PlaylistErrorCode.SUBSCRIPTION_NOT_FOUND, new HashMap<>());}

    public static SubscriptionNotFoundException withId(UUID subscriberId, UUID playlistId) {
        SubscriptionNotFoundException ex = new SubscriptionNotFoundException();
        ex.getDetails().put("playlistId", playlistId);
        ex.getDetails().put("subscriberId", subscriberId);
        return ex;
    }
}
