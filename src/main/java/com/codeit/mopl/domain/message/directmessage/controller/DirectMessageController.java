package com.codeit.mopl.domain.message.directmessage.controller;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSendRequest;
import com.codeit.mopl.domain.message.directmessage.service.DirectMessageService;
import com.codeit.mopl.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/*
  실시간 DirectMessage 송수신 (WebSocket)
  - 실시간으로 다른 사용자와 1대1 DirectMessage를 주고받을 수 있게하는 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public void sendDirectMessage (@DestinationVariable UUID conversationId,
                                   @Payload DirectMessageSendRequest request,
                                   Authentication authentication
    ) {
        log.info("[WS CONTROLLER] sendDirectMessage called, authentication={}", authentication);
        UUID senderId = null;

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            senderId = userDetails.getUser().id();
            log.info("[WS CONTROLLER] senderId extracted: {}", senderId);
        } else {
            log.warn("[WS CONTROLLER] authentication is null or principal is not CustomUserDetails, authentication={}", authentication);
        }

        DirectMessageDto dto =
                directMessageService.saveDirectMessage(senderId, conversationId, request);

        String destination = "/sub/conversations/" + conversationId + "/direct-messages";
        log.info("[WS SEND] Sending message to destination: {}, dto: {}", destination, dto);
        log.info("[WS SEND] messagingTemplate class: {}", messagingTemplate.getClass().getName());
        try {
            messagingTemplate.convertAndSend(destination, dto);
            log.info("[WS SEND] Message sent successfully to: {}", destination);
        } catch (Exception e) {
            log.error("[WS SEND] Failed to send message to: {}, error: {}", destination, e.getMessage(), e);
            throw e;
        }
    }
}
