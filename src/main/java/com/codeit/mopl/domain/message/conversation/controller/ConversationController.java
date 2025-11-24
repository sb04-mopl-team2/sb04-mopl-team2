package com.codeit.mopl.domain.message.conversation.controller;

import com.codeit.mopl.domain.message.conversation.dto.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.ConversationDto;
import com.codeit.mopl.domain.message.conversation.service.ConversationService;
import com.codeit.mopl.domain.message.directmessage.service.DirectMessageService;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;
    private final DirectMessageService directMessageService;

    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
            @Valid @RequestBody ConversationCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails loginUser) {
        log.info("[메세지] 채팅방 생성 요청 - conversationWithId = {}", request.withUserId());
        ConversationDto response = conversationService.createConversation(loginUser.getUser().id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
