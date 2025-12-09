package com.codeit.mopl.domain.watchingsession.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.watchingsession.dto.ContentChatDto;
import com.codeit.mopl.domain.watchingsession.dto.MessagePayload;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.domain.watchingsession.entity.enums.ChangeType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

@ExtendWith(MockitoExtension.class)
public class RedisPublisherTest {

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ChannelTopic topic;

  @InjectMocks
  private RedisPublisher redisPublisher;

  @Test
  @DisplayName("ContentChatDto로 publish 성공")
  void publishToDestinationSuccessWithContentChatDto() {
    // given
    UUID contentId = UUID.randomUUID();
    String destination = "/sub/contents/" + contentId + "/watch";
    ContentChatDto contentChatDto = new ContentChatDto(
        mock(UserSummary.class),
        "test"
    );
    when(topic.getTopic()).thenReturn("websocket-events");

    // when
    redisPublisher.convertAndSend(destination, contentChatDto);

    // then
    ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
    verify(redisTemplate, times(1)).convertAndSend(
        eq("websocket-events"),
        payloadCaptor.capture()
    );
    MessagePayload capturedPayload = payloadCaptor.getValue();
    assertThat(capturedPayload.destination()).isEqualTo(destination);
    assertThat(capturedPayload.content()).isEqualTo(contentChatDto);
  }

  @Test
  @DisplayName("WatchingSessionChange로 publish 성공")
  void publishToDestinationSuccessWithWatchingSessionChange() {
    // given
    UUID contentId = UUID.randomUUID();
    String destination = "/sub/contents/" + contentId + "/watch";
    WatchingSessionChange watchingSessionChange = new WatchingSessionChange(
        ChangeType.JOIN,
        mock(WatchingSessionDto.class),
        0L
    );
    when(topic.getTopic()).thenReturn("websocket-events");

    // when
    redisPublisher.convertAndSend(destination, watchingSessionChange);

    // then
    ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
    verify(redisTemplate, times(1)).convertAndSend(
        eq("websocket-events"),
        payloadCaptor.capture()
    );
    MessagePayload capturedPayload = payloadCaptor.getValue();
    assertThat(capturedPayload.destination()).isEqualTo(destination);
    assertThat(capturedPayload.content()).isEqualTo(watchingSessionChange);
  }
}
