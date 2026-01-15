package com.codeit.mopl.domain.playlist.dto;

import com.codeit.mopl.domain.content.dto.response.ContentSummary;
import com.codeit.mopl.domain.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistCachedDto(
        UUID id,
        UserSummary owner,
        String title,
        String description,
        Instant updatedAt,
        long subscriberCount,
        List<ContentSummary> contents
) {
}
