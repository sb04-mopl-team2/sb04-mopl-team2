package com.codeit.mopl.domain.message.conversation.controller;

import com.codeit.mopl.domain.message.conversation.dto.request.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.dto.response.ConversationDto;
import com.codeit.mopl.domain.message.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.mopl.domain.message.conversation.service.ConversationService;
import com.codeit.mopl.domain.message.directmessage.dto.CursorResponseDirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSearchCond;
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

import java.util.UUID;

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
        log.info("[메세지] 채팅방 생성 요청 - WithUserId = {}", request.withUserId());
        ConversationDto response = conversationService.createConversation(loginUser.getUser().id(), request);
        log.info("[메세지] 채팅방 생성 응답 - WithUserId = {}", response.with().userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CursorResponseConversationDto> getConversations(@AuthenticationPrincipal CustomUserDetails loginUser,
                                                                          @Validated @ModelAttribute ConversationSearchCond request) {
        log.info("[메세지] 채팅방 목록 조회 요청 - loginUser = {}", loginUser.getUser().id());
        CursorResponseConversationDto response = conversationService.getAllConversations(loginUser.getUser().id(), request);
        log.info("[메세지] 채팅방 목록 조회 응답 - loginUser = {}", loginUser.getUser().id());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable UUID conversationId,
                                                           @AuthenticationPrincipal CustomUserDetails loginUser) {
        log.info("[메세지] 채팅방 정보 조회 요청 - conversationId = {}", conversationId);
        ConversationDto response = conversationService.getConversationById( loginUser.getUser().id() ,conversationId);
        log.info("[메세지] 채팅방 정보 조회 응답 - conversationId = {}", conversationId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/with")
    public ResponseEntity<ConversationDto> getConversationByUserId(@RequestParam UUID userId,
                                                                   @AuthenticationPrincipal CustomUserDetails loginUser) {
        log.info("[메세지] 특정 사용자와의 채팅방 조회 요청 - withUserId = {}", userId);
        ConversationDto response = conversationService.getConversationByUserId(loginUser.getUser().id(), userId);
        log.info("[메세지] 특정 사용자와의 채팅방 조회 응답 - withUserId = {}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID conversationId,
                                         @PathVariable UUID directMessageId,
                                         @AuthenticationPrincipal CustomUserDetails loginUser) {
        log.info("[메세지] DM '읽음' 처리 요청 - conversationId = {}, directMessageId = {}", conversationId, directMessageId);
        conversationService.markAsRead(loginUser.getUser().id(), conversationId, directMessageId);
        log.info("[메세지] DM '읽음' 처리 응답 - conversationId = {}, directMessageId = {}", conversationId, directMessageId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{conversationId}/direct-messages")
    public ResponseEntity<CursorResponseDirectMessageDto> getDirectMessages(@PathVariable UUID conversationId,
                                                                           @AuthenticationPrincipal CustomUserDetails loginUser,
                                                                           @Validated @ModelAttribute DirectMessageSearchCond request) {
        log.info("[메세지] 해당 채팅방의 DM 목록 조회 요청 - conversationId = {}", conversationId);
        CursorResponseDirectMessageDto response = directMessageService.getDirectMessages(loginUser.getUser().id(), conversationId, request);
        log.info("[메세지] 해당 채팅방의 DM 목록 조회 응답 - conversationId = {}", conversationId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
