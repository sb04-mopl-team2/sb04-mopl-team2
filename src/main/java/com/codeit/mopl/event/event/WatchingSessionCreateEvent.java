package com.codeit.mopl.event.event;

import java.util.UUID;

public record WatchingSessionCreateEvent(UUID watchingSessionId, UUID ownerId, String watchingSessionContentTitle){

}