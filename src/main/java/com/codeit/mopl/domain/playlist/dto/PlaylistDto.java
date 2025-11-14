package com.codeit.mopl.domain.playlist.dto;


import com.codeit.mopl.domain.user.dto.response.UserSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PlaylistDto (
    UUID id,
    UserSummary owner,
    String title,
    String description,
    LocalDateTime updatedAt,
    long subscriberCount,
    boolean subscribedByMe
    List<ContentSummary> contents // ContentSummary가 아직 생성되지 않은 듯해서 추후에 확인하여 import할 예정입니다.
)
{ }
