package com.codeit.mopl.event;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.listener.KafkaEventListener;
import com.codeit.mopl.exception.follow.FollowIdIsNullException;
import com.codeit.mopl.exception.follow.FolloweeIdIsNullException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventListenerTest {

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  @Mock
  private ObjectMapper objectMapper;

  private KafkaEventListener kafkaEventListener;

  @BeforeEach
  void setUp() {
    kafkaEventListener = new KafkaEventListener(kafkaTemplate, objectMapper);
    MDC.clear();
  }

  @Test
  @DisplayName("NotificationCreateEvent 발생 시 JSON 직렬화 후 Kafka 전송, 헤더 포함")
  void onNotificationCreateEvent_shouldSendKafkaMessageWithHeaders() throws Exception {
    // given
    NotificationDto dto = mock(NotificationDto.class);
    UUID id = UUID.randomUUID();

    when(dto.id()).thenReturn(id);

    NotificationCreateEvent event = mock(NotificationCreateEvent.class);
    when(event.notificationDto()).thenReturn(dto);

    String expectedJson = "{\"test\":\"json\"}";
    when(objectMapper.writeValueAsString(event)).thenReturn(expectedJson);

    // KafkaTemplate.send(...) 이 CompletableFuture 를 반환하도록 stubbing
    @SuppressWarnings("unchecked")
    CompletableFuture<SendResult<String, String>> future =
        (CompletableFuture<SendResult<String, String>>) mock(CompletableFuture.class);
    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

    // MDC traceId 세팅
    String traceId = "trace-123";
    MDC.put("requestId", traceId);

    // when
    kafkaEventListener.on(event);

    // then
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor =
        ArgumentCaptor.forClass(ProducerRecord.class);

    verify(kafkaTemplate, times(1)).send(recordCaptor.capture());

    ProducerRecord<String, String> sentRecord = recordCaptor.getValue();

    // topic / key / value 검증
    assertThat(sentRecord.topic()).isEqualTo("mopl-notification-create");
    assertThat(sentRecord.key()).isEqualTo(id.toString());
    assertThat(sentRecord.value()).isEqualTo(expectedJson);

    // 헤더 검증
    Headers headers = sentRecord.headers();

    Header traceIdHeader = headers.lastHeader("x-trace-id");
    assertThat(traceIdHeader).isNotNull();
    assertThat(new String(traceIdHeader.value(), StandardCharsets.UTF_8))
        .isEqualTo(traceId);

    Header eventTypeHeader = headers.lastHeader("x-event-type");
    assertThat(eventTypeHeader).isNotNull();
    assertThat(new String(eventTypeHeader.value(), StandardCharsets.UTF_8))
        .isEqualTo(event.getClass().getSimpleName());
  }

  @Test
  @DisplayName("FollowerIncreaseEvent 발생 시 Kafka 메시지 전송 성공")
  void onFollowerIncreaseEvent_shouldSendKafkaMessageWithHeaders() throws Exception {
    // given
    UUID followId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowerIncreaseEvent event = new FollowerIncreaseEvent(followId, followeeId);

    String expectedJson = "{\"event\":\"increase\"}";
    when(objectMapper.writeValueAsString(event)).thenReturn(expectedJson);

    @SuppressWarnings("unchecked")
    CompletableFuture<SendResult<String, String>> future =
            (CompletableFuture<SendResult<String, String>>) mock(CompletableFuture.class);
    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

    MDC.put("requestId", "trace-456");

    // when
    kafkaEventListener.on(event);

    // then
    ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());

    ProducerRecord<String, String> record = captor.getValue();
    assertThat(record.topic()).isEqualTo("mopl-follower-increase");
    assertThat(record.key()).isEqualTo(followeeId.toString());
    assertThat(record.value()).isEqualTo(expectedJson);

    Header typeHeader = record.headers().lastHeader("x-event-type");
    assertThat(typeHeader).isNotNull();
    assertThat(new String(typeHeader.value(), StandardCharsets.UTF_8))
            .isEqualTo(event.getClass().getSimpleName());
  }

  @Test
  @DisplayName("FollowerIncreaseEvent Kafka 메시지 전송 실패 - followId는 null이 될 수 없음")
  void onFollowerIncreaseEvent_FollowIdIsNull_ThrowsException() {
    // given
    FollowerIncreaseEvent event = new FollowerIncreaseEvent(null, null);

    // when & then
    assertThatThrownBy(() -> kafkaEventListener.on(event))
            .isInstanceOf(FollowIdIsNullException.class);
    verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
  }

  @Test
  @DisplayName("FollowerIncreaseEvent Kafka 메시지 전송 실패 - followeeId는 null이 될 수 없음")
  void onFollowerIncreaseEvent_FolloweeIdIsNull_ThrowsException() {
    // given
    UUID followId = UUID.randomUUID();
    FollowerIncreaseEvent event = new FollowerIncreaseEvent(followId, null);

    // when & then
    assertThatThrownBy(() -> kafkaEventListener.on(event))
            .isInstanceOf(FolloweeIdIsNullException.class);
    verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
  }
}
