package com.codeit.mopl.domain.watchingsession.controller;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.watchingsession.dto.ContentChatDto;
import com.codeit.mopl.domain.watchingsession.entity.ContentChatSendRequest;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.exception.user.ErrorCode;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/*
  컨텐츠 실시간 채팅 (WebSocket)
  - 라이브 채팅 메시지를 받아 모든 시청자에게 중계하는 컨트롤러
  - Service 거치지 않음
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/contents/{contentId}/chat") // adds /pub
  public void sendChat(@DestinationVariable String contentId,
      @Payload ContentChatSendRequest contentChatSendRequest,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    UserDto userDto = principal.getUser();

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
    String destination = String.format("/sub/contents/%s/chat", contentId);
    messagingTemplate.convertAndSend(destination, contentChatDto);
  }

}
