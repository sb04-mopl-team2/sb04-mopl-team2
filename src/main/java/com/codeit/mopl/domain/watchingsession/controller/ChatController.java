package com.codeit.mopl.domain.watchingsession.controller;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.watchingsession.dto.ContentChatDto;
import com.codeit.mopl.domain.watchingsession.entity.ContentChatSendRequest;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.service.RedisPublisher;
import com.codeit.mopl.exception.watchingsession.UserNotAuthenticatedException;
import com.codeit.mopl.exception.watchingsession.WatchingSessionErrorCode;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

/*
  컨텐츠 실시간 채팅 (WebSocket)
  - 라이브 채팅 메시지를 받아 모든 시청자에게 중계하는 컨트롤러
  - Service 거치지 않음
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

//  private final SimpMessagingTemplate messagingTemplate;
  private final RedisPublisher redisPublisher;

  // (/pub 추가) client -> server
  // 엔드포인트: SEND /pub/contents/{contentId}/chat
  @MessageMapping("/contents/{contentId}/chat")
  public void sendChat(@DestinationVariable UUID contentId,
                       @Valid @Payload ContentChatSendRequest contentChatSendRequest,
                       UsernamePasswordAuthenticationToken token
  ) {

     if (token == null || !(token.getPrincipal() instanceof CustomUserDetails userDetails)) {
       throw new UserNotAuthenticatedException(
          WatchingSessionErrorCode.USER_NOT_AUTHENTICATED,
          Map.of("contentId", contentId));
    }
    log.info("[실시간 채팅] Chat Controller - 유저 정보 받음. UserDto = {}", userDetails.getUser());

    UserDto userDto = userDetails.getUser();
    UserSummary senderSummary = new UserSummary(
        userDto.id(),
        userDto.name(),
        userDto.profileImageUrl()
    );
    ContentChatDto contentChatDto = new ContentChatDto(
        senderSummary,
        contentChatSendRequest.content()
    );

    // server -> client
    // 엔드포인트: SUBSCRIBE /sub/contents/{contentId}/chat
    String destination = String.format("/sub/contents/%s/chat", contentId);
    redisPublisher.convertAndSend(destination, contentChatDto);
//    messagingTemplate.convertAndSend(destination, contentChatDto);
    log.info("[실시간 채팅] Chat Controller - 채팅 보냄. destination = {}", destination);
  }
}
