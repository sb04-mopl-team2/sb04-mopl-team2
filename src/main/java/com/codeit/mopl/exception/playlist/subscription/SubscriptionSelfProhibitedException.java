package com.codeit.mopl.exception.playlist.subscription;

import com.codeit.mopl.exception.playlist.PlaylistErrorCode;
import com.codeit.mopl.exception.playlist.PlaylistException;

import java.util.HashMap;
import java.util.UUID;

public class SubscriptionSelfProhibitedException extends PlaylistException {
    private SubscriptionSelfProhibitedException() { super(PlaylistErrorCode.SELF_SUBSCRIPTION_FORBIDDEN, new HashMap<>()); }

    public static SubscriptionSelfProhibitedException withId(UUID playlistId,UUID subscriberId) {
        SubscriptionSelfProhibitedException ex = new SubscriptionSelfProhibitedException();
        ex.getDetails().put("playlistId", playlistId);
        ex.getDetails().put("subscriberId", subscriberId);
        return ex;
    }
}
