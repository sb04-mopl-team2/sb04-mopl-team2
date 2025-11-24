package com.codeit.mopl.exception.playlist.subscription;

import com.codeit.mopl.exception.playlist.PlaylistErrorCode;
import com.codeit.mopl.exception.playlist.PlaylistException;

import java.util.HashMap;
import java.util.UUID;

public class SubscriptionDuplicateException extends PlaylistException {
    private SubscriptionDuplicateException() { super(PlaylistErrorCode.SUBSCRIPTION_ALREADY_EXISTS, new HashMap<>()); }

    public static SubscriptionDuplicateException withId(UUID playlistId, UUID subscriberId) {
        SubscriptionDuplicateException ex = new SubscriptionDuplicateException();
        ex.getDetails().put("playlistId", playlistId);
        ex.getDetails().put("subscriberId", subscriberId);
        return ex;
    }
}
