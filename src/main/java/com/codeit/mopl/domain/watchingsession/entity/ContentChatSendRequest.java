package com.codeit.mopl.domain.watchingsession.entity;

import jakarta.validation.constraints.NotBlank;

public record ContentChatSendRequest(
    @NotBlank
    String content
) {

}
