package com.codeit.mopl.event;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.consumer.KafkaConsumer;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.support.Acknowledgment;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
  @DisplayName("정상 이벤트 처리 - 아직 처리되지 않은 이벤트면 notificationService 호출 후 processedEvent 저장, ack 호출")
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

    // 아직 처리되지 않은 이벤트
    when(processedEventRepository.findByEventIdAndEventType(eventId, EventType.NOTIFICATION_CREATED))
        .thenReturn(Optional.empty());

    // when
    kafkaConsumer.onNotificationCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(eventId, EventType.NOTIFICATION_CREATED);
    verify(notificationService).sendNotification(notificationDto);
    verify(processedEventRepository).save(any(ProcessedEvent.class));
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("이미 처리된 이벤트 - 조회 결과 존재하면 notificationService 호출 안 하고 ack만 호출")
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

    // 이미 처리된 이벤트(조회 시 Optional.of)
    when(processedEventRepository.findByEventIdAndEventType(eventId, EventType.NOTIFICATION_CREATED))
        .thenReturn(Optional.of(new ProcessedEvent(eventId, EventType.NOTIFICATION_CREATED)));

    // when
    kafkaConsumer.onNotificationCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(eventId, EventType.NOTIFICATION_CREATED);
    // 새로운 저장/서비스 호출 없음
    verify(processedEventRepository, never()).save(any());
    verify(notificationService, never()).sendNotification(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("JSON 역직렬화 실패 - repository, service 모두 호출 안 하고 ack만 호출")
  void onNotificationCreated_jsonDeserializeFail() throws Exception {
    // given
    String invalidJson = "INVALID_JSON";

    when(objectMapper.readValue(invalidJson, NotificationCreateEvent.class))
        .thenThrow(new JsonProcessingException("fail") {});

    // when
    kafkaConsumer.onNotificationCreated(invalidJson, ack);

    // then
    verify(processedEventRepository, never()).findByEventIdAndEventType(any(), any());
    verify(processedEventRepository, never()).save(any());
    verify(notificationService, never()).sendNotification(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("기타 예외 발생 - ack 호출하지 않고 예외 전파")
  void onNotificationCreated_unexpectedException() throws Exception {
    // given
    String json = "{\"id\":\"dummy\"}";
    UUID eventId = UUID.randomUUID();

    when(objectMapper.readValue(json, NotificationCreateEvent.class))
        .thenReturn(notificationCreateEvent);
    when(notificationCreateEvent.notificationDto())
        .thenReturn(notificationDto);
    when(notificationDto.id())
        .thenReturn(eventId);

    when(processedEventRepository.findByEventIdAndEventType(eventId, EventType.NOTIFICATION_CREATED))
        .thenReturn(Optional.empty());

    // notificationService 내부에서 예외 발생한다고 가정
    doThrow(new RuntimeException("unexpected"))
        .when(notificationService).sendNotification(notificationDto);

    // when & then
    assertThatThrownBy(() -> kafkaConsumer.onNotificationCreated(json, ack))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("unexpected");

    verify(ack, never()).acknowledge();
  }
}
