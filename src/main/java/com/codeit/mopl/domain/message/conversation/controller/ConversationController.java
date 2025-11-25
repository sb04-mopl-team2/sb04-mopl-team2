package com.codeit.mopl.domain.message.conversation.controller;

import com.codeit.mopl.domain.message.conversation.dto.request.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.dto.response.ConversationDto;
import com.codeit.mopl.domain.message.conversation.dto.response.CursorResponseConversationDto;
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
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<CursorResponseConversationDto> getConversations(@AuthenticationPrincipal CustomUserDetails loginUser,
                                                                          @Validated @ModelAttribute ConversationSearchCond request) {
        log.info("[메세지] 채팅방 목록 조회 요청 - loginUser = {}", loginUser.getUser().id());
        CursorResponseConversationDto response = conversationService.getAllConversations(loginUser.getUser().id(), request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
