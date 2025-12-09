package com.codeit.mopl.domain.notification.template.context;

public record PlaylistSubscribedContext(
    String username, String playlistTitle
) {}