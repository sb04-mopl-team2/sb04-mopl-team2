package com.codeit.mopl.domain.watchingsession.controller;

import com.codeit.mopl.domain.watchingsession.dto.ContentChatDto;
import com.codeit.mopl.domain.watchingsession.entity.ContentChatSendRequest;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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

  /**
   * 콘텐츠 실시간 채팅
   */
  @MessageMapping("/contents/{contentId}/chat") // adds /pub
  public void sendChat(@DestinationVariable String contentId,
      @Payload ContentChatSendRequest contentChatSendRequest,
      @AuthenticationPrincipal User principal // 나중에 MoplUserDetail로 바꾸기
  ) {
//    UserSummary senderSummary = new UserSummary(
//        principal.getId(),
//        principal.getUsername(),
//        principal.getProfileImageUrl()
//    );
    ContentChatDto contentChatDto = new ContentChatDto(
//        senderSummary,
        null,
        contentChatSendRequest.content()
    );

    // server -> client
    String destination = String.format("/sub/contents/%s/chat", contentId);
    messagingTemplate.convertAndSend(destination, contentChatDto);
  }

}
