package com.codeit.mopl.domain.notification.template.context;

public record RoleChangedContext(
    String beforeRole, String afterRole
) {}