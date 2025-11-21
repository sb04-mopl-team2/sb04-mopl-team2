package com.codeit.mopl.event;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.consumer.KafkaConsumer;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private NotificationService notificationService;

  @Mock
  private ProcessedEventRepository processedEventRepository;

  @Mock
  private Acknowledgment ack;

  @Mock
  private NotificationCreateEvent notificationCreateEvent;

  @Mock
  private NotificationDto notificationDto;

  private KafkaConsumer kafkaConsumer;


  @BeforeEach
  void setUp() {
    kafkaConsumer = new KafkaConsumer(objectMapper, notificationService, processedEventRepository);
  }

  @Test
  @DisplayName("정상 이벤트 처리 - processedEvent 저장 후 notificationService 호출, ack 호출")
  void onNotificationCreated_success() throws Exception {
    // given
    String json = "{\"id\":\"dummy\"}";
    UUID eventId = UUID.randomUUID();

    when(objectMapper.readValue(json, NotificationCreateEvent.class))
        .thenReturn(notificationCreateEvent);
    when(notificationCreateEvent.notificationDto())
        .thenReturn(notificationDto);
    when(notificationDto.id())
        .thenReturn(eventId);

    // when
    kafkaConsumer.onNotificationCreated(json, ack);

    // then
    // 멱등성 저장 시도
    verify(processedEventRepository).save(any(ProcessedEvent.class));
    verify(notificationService).sendNotification(notificationDto);
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("이미 처리된 이벤트 - DataIntegrityViolation 발생 시 notificationService 호출 안 함, ack 호출")
  void onNotificationCreated_idempotent() throws Exception {
    // given
    String json = "{\"id\":\"dummy\"}";
    UUID eventId = UUID.randomUUID();

    when(objectMapper.readValue(json, NotificationCreateEvent.class))
        .thenReturn(notificationCreateEvent);
    when(notificationCreateEvent.notificationDto())
        .thenReturn(notificationDto);
    when(notificationDto.id())
        .thenReturn(eventId);

    doThrow(new DataIntegrityViolationException("duplicate"))
        .when(processedEventRepository).save(any(ProcessedEvent.class));

    // when
    kafkaConsumer.onNotificationCreated(json, ack);

    // then
    // 이미 처리된 이벤트이므로 서비스는 호출 안 됨
    verify(notificationService, never()).sendNotification(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("JSON 역직렬화 실패 - notificationService 호출 안 하고 ack 호출")
  void onNotificationCreated_jsonDeserializeFail() throws Exception {
    // given
    String invalidJson = "INVALID_JSON";

    when(objectMapper.readValue(invalidJson, NotificationCreateEvent.class))
        .thenThrow(new JsonProcessingException("fail") {});

    // when
    kafkaConsumer.onNotificationCreated(invalidJson, ack);

    // then
    verify(processedEventRepository, never()).save(any());
    verify(notificationService, never()).sendNotification(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("기타 예외 발생 - ack 호출하지 않고 예외 전파")
  void onNotificationCreated_unexpectedException() throws Exception {
    // given
    String json = "{\"id\":\"dummy\"}";

    when(objectMapper.readValue(json, NotificationCreateEvent.class))
        .thenReturn(notificationCreateEvent);
    when(notificationCreateEvent.notificationDto())
        .thenThrow(new RuntimeException("unexpected"));

    // when & then
    assertThatThrownBy(() -> kafkaConsumer.onNotificationCreated(json, ack))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("unexpected");

    verify(ack, never()).acknowledge();
  }
}
