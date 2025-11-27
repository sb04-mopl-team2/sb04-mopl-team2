package com.codeit.mopl.event;

import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.event.consumer.FollowEventKafkaConsumer;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowEventKafkaConsumerTest {

    @InjectMocks
    FollowEventKafkaConsumer followEventKafkaConsumer;

    @Mock
    FollowService followService;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    private Acknowledgment ack;

    @Test
    @DisplayName("팔로워 증가 이벤트 처리 성공")
    void onFollowerIncrease_Success() throws Exception {
        // given
        String json = "{...}";
        UUID followId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowerIncreaseEvent event = new FollowerIncreaseEvent(followId, followeeId);

        given(objectMapper.readValue(json, FollowerIncreaseEvent.class)).willReturn(event);

        // when
        followEventKafkaConsumer.onFollowerIncrease(json, ack);

        // then
        verify(followService).processFollowerIncrease(eq(followId), eq(followeeId));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("팔로워 증가 이벤트 처리 실패 - JSON 역직렬화 실패 시 팔로워 증가시키지 않고 ack 호출")
    void onFollowerIncrease_JsonDeserializeFail() throws Exception {
        // given
        String json = "{invalid json}";
        given(objectMapper.readValue(json, FollowerIncreaseEvent.class))
                .willThrow(JsonProcessingException.class);

        // when
        followEventKafkaConsumer.onFollowerIncrease(json, ack);

        // then
        verify(followService, never()).processFollowerIncrease(any(), any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("팔로워 증가 이벤트 처리 실패 - 서비스 예외 발생시 ack 호출하지 않음")
    void onFollowerIncrease_ServiceFail_ShouldThrowException() throws Exception {
        // given
        String json = "{...}";
        UUID followId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowerIncreaseEvent event = new FollowerIncreaseEvent(followId, followeeId);

        given(objectMapper.readValue(json, FollowerIncreaseEvent.class))
                .willReturn(event);

        doThrow(new RuntimeException("service error"))
                .when(followService)
                .processFollowerIncrease(followId, followeeId);

        // when & then
        assertThatThrownBy(() -> followEventKafkaConsumer.onFollowerIncrease(json, ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }

    @Test
    @DisplayName("팔로워 감소 이벤트 처리 성공")
    void onFollowerDecrease_Success() throws Exception {
        // given
        String json = "{...}";
        UUID followId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowerDecreaseEvent event = new FollowerDecreaseEvent(followId, followeeId);

        given(objectMapper.readValue(json, FollowerDecreaseEvent.class))
                .willReturn(event);

        // when
        followEventKafkaConsumer.onFollowerDecrease(json, ack);

        // then
        verify(followService).processFollowerDecrease(eq(followId), eq(followeeId));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("팔로워 감소 이벤트 처리 실패 - JSON 역직렬화 실패 시 팔로워 감소시키지 않고 ack 호출")
    void onFollowerDecrease_JsonDeserializeFail() throws Exception {
        // given
        String json = "{invalid json}";
        given(objectMapper.readValue(json, FollowerDecreaseEvent.class))
                .willThrow(JsonProcessingException.class);

        // when
        followEventKafkaConsumer.onFollowerDecrease(json, ack);

        // then
        verify(followService, never()).processFollowerIncrease(any(), any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("팔로워 감소 이벤트 처리 실패 - 서비스 예외 발생 시 ack 호출하지 않음")
    void onFollowerDecrease_ServiceFail_ShouldThrowException() throws Exception {
        // given
        String json = "{...}";
        UUID followId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowerDecreaseEvent event = new FollowerDecreaseEvent(followId, followeeId);

        given(objectMapper.readValue(json, FollowerDecreaseEvent.class))
                .willReturn(event);

        doThrow(new RuntimeException("service error"))
                .when(followService)
                .processFollowerDecrease(eq(followId), eq(followeeId));

        // when & then
        assertThatThrownBy(() -> followEventKafkaConsumer.onFollowerDecrease(json, ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }
}