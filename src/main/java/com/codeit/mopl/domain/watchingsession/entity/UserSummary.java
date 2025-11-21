package com.codeit.mopl.domain.watchingsession.entity;

import java.util.UUID;

public record UserSummary(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
