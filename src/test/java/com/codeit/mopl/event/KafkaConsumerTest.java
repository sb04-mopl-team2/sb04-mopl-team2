package com.codeit.mopl.event;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.consumer.KafkaConsumer;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private NotificationService notificationService;

  @Mock
  private Acknowledgment acknowledgment;

  private KafkaConsumer kafkaConsumer;

  @BeforeEach
  void setUp() {
    kafkaConsumer = new KafkaConsumer(objectMapper, notificationService);
  }

  @Test
  @DisplayName("정상적인 Kafka 메시지일 때 JSON 이벤트 변환, sendNotification 호출, ack 호출")
  void onNotificationCreated_success() throws Exception {
    // given
    String kafkaEventJson = "{\"test\":\"value\"}";

    // NotificationDto / NotificationCreateEvent 는 실제 구현을 모를 수 있으니 mock 으로 처리
    NotificationDto notificationDto = mock(NotificationDto.class);
    NotificationCreateEvent event = mock(NotificationCreateEvent.class);

    when(objectMapper.readValue(kafkaEventJson, NotificationCreateEvent.class))
        .thenReturn(event);
    when(event.notificationDto()).thenReturn(notificationDto);

    // when
    kafkaConsumer.onNotificationCreated(kafkaEventJson, acknowledgment);

    // then
    verify(notificationService, times(1)).sendNotification(notificationDto);
    verify(acknowledgment, times(1)).acknowledge();
    verifyNoMoreInteractions(notificationService, acknowledgment);
  }

}
