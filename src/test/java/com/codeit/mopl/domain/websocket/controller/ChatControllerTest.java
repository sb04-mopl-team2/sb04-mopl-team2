package com.codeit.mopl.domain.websocket.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.watchingsession.controller.ChatController;
import com.codeit.mopl.domain.watchingsession.dto.ContentChatDto;
import com.codeit.mopl.domain.watchingsession.entity.ContentChatSendRequest;
import com.codeit.mopl.domain.watchingsession.service.RedisPublisher;
import com.codeit.mopl.security.CustomUserDetails;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {

  @Mock
  private RedisPublisher redisPublisher;

  @InjectMocks
  private ChatController chatController;

  @DisplayName("웹소켓 실시간 채팅 client -> server로 보내는 시도를 한다")
  @Test
  void sendChatSuccess() {
    // given
    UUID contentId = UUID.randomUUID();
    String destination = "/sub/contents/" + contentId + "/chat";
    ContentChatSendRequest contentChatSendRequest = new ContentChatSendRequest("testContent");
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(
        userId,
        Instant.now(),
        "test@test.com",
        "test",
        null,
        Role.USER,
        false
    );
    CustomUserDetails customUserDetails = new CustomUserDetails(
        userDto,
        "testPassword"
    );
    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(
            customUserDetails,
            null, // 비번은 필요 없음
            customUserDetails.getAuthorities()
        );

    // when
    chatController.sendChat(contentId, contentChatSendRequest, authenticationToken);

    // then
    ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<ContentChatDto> payloadCaptor = ArgumentCaptor.forClass(ContentChatDto.class);

    verify(redisPublisher).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());
    ContentChatDto sentMessage = payloadCaptor.getValue();
    String sentDestination = destinationCaptor.getValue();

    assertThat(sentDestination).isEqualTo(destination);
    assertThat(sentMessage.content()).isEqualTo("testContent");
    assertThat(sentMessage.sender().userId()).isEqualTo(userId);
    assertThat(sentMessage.sender().name()).isEqualTo("test");
  }
}
