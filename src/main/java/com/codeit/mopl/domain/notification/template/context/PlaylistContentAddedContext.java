package com.codeit.mopl.domain.notification.template.context;

public record PlaylistContentAddedContext(
    String playlistTitle, String contentTitle
) {}