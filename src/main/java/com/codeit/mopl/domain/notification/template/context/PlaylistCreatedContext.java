package com.codeit.mopl.domain.notification.template.context;

public record PlaylistCreatedContext(
    String username, String playlistTitle
) {}