package com.codeit.mopl.domain.notification.template;

public record NotificationContext(
    String username,
    String target,
    String playlist,
    String content
) {

}
