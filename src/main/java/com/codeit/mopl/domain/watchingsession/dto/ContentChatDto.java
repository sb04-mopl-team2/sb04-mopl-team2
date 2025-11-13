package com.codeit.mopl.domain.watchingsession.dto;

import com.codeit.mopl.domain.watchingsession.entity.UserSummary;

public record ContentChatDto(
    UserSummary sender,
    String content
) {

}
