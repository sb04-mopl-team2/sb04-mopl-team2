package com.codeit.mopl.domain.message.directmessage.controller;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSendRequest;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.message.directmessage.service.DirectMessageService;
import com.codeit.mopl.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                                   DirectMessageSendRequest request,
                                   @AuthenticationPrincipal CustomUserDetails loginUser) {
        DirectMessageDto dto = directMessageService.saveDirectMessage(loginUser.getUser().id(), request);
        messagingTemplate.convertAndSend(
                "/sub/conversations/" + conversationId + "/direct-messages", dto);
    }
}
