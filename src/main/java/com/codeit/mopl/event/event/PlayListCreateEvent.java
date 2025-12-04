package com.codeit.mopl.event.event;

import java.util.UUID;

public record PlayListCreateEvent(UUID playListId, UUID ownerId, String title){

}