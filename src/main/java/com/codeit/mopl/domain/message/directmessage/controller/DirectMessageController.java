package com.codeit.mopl.domain.message.directmessage.controller;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSendRequest;
import com.codeit.mopl.domain.message.directmessage.service.DirectMessageService;
import com.codeit.mopl.exception.message.MessageErrorCode;
import com.codeit.mopl.exception.message.directmessage.UserNotAuthenticatedException;
import com.codeit.mopl.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.util.Map;
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
                                   UsernamePasswordAuthenticationToken token) {

        if (token == null || !(token.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UserNotAuthenticatedException(MessageErrorCode.USER_NOT_AUTHENTICATED,
                    Map.of("conversationId",conversationId));
        }

        DirectMessageDto dto = directMessageService.saveDirectMessage(userDetails.getUser().id(),conversationId,request);
        messagingTemplate.convertAndSend(
                "/sub/conversations/" + conversationId + "/direct-messages", dto);
    }
}
