package com.codeit.mopl.domain.playlist.dto;


import com.codeit.mopl.domain.content.entity.Content;

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
        boolean subscribedByMe,
        List<ContentSummary> contents

        //아직 생성되지 않은 dto 가 있어 추후에 따로 확인하여 import할 예정입니다.
)
{ }
