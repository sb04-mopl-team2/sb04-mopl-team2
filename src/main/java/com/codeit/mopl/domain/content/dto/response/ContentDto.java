package com.codeit.mopl.domain.content.dto.response;

import java.util.List;
import java.util.UUID;

public record ContentDto(
    UUID id,
    String type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    Double averageRating,
    Integer reviewCount,
    Long watcherCount
) {}
