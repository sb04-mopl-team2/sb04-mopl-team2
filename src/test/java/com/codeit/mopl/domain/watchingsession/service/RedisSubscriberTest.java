package com.codeit.mopl.domain.watchingsession.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
public class RedisSubscriberTest {

  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private ObjectMapper mapper;

  @InjectMocks
  private RedisSubscriber redisSubscriber;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    redisSubscriber = new RedisSubscriber(template, mapper);
  }

  @Test
  @DisplayName("subscribe로 메세지 수신 성공")
  void subscribedSuccessWithStringMessage() throws JsonProcessingException {
    // given
    String jsonMessage = """
            {
                "destination": "/sub/contents/00042833-dba0-4762-acc3-5f47e2e87509/watch",
                "content": {
                    "type": "JOIN",
                    "watchingSession": {
                        "id": "16924bae-fdb4-434e-b2e3-6dfc6cc77c88",
                        "createdAt": "2025-12-05T16:37:13.0659413",
                        "watcher": {
                            "userId": "678793de-87d9-440b-bef0-b36361163bc3",
                            "name": "test@test.com",
                            "profileImageUrl": null
                        },
                        "content": {
                            "id": "00042833-dba0-4762-acc3-5f47e2e87509",
                            "title": "겹",
                            "tags": ["애니메이션"]
                        }
                    },
                    "watcherCount": 2
                }
            }
            """;

    // when
    redisSubscriber.onMessage(jsonMessage);

    // then
    verify(template, times(1)).convertAndSend(
        eq("/sub/contents/00042833-dba0-4762-acc3-5f47e2e87509/watch"),
        any(Object.class)
    );
  }

  @Test
  @DisplayName("Invalid JSON으로 message subscribe 실패")
  void subscribedFailureInvalidJson() throws JsonProcessingException {
    // given
    String jsonMessage = "{ \"invalid\" : ";

    // when
    redisSubscriber.onMessage(jsonMessage);

    // then
    verify(template, never()).convertAndSend(any(String.class), (Object) any());
  }
}
