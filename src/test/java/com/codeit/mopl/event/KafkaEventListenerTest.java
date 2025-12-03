package com.codeit.mopl.event;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.consumer.KafkaConsumer;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.DirectMessageCreateEvent;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.listener.KafkaEventListener;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventListenerTest {

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  @Mock
  private ObjectMapper objectMapper;

  private KafkaEventListener kafkaEventListener;

  @Mock
  private ProcessedEventRepository processedEventRepository;

  @Mock
  private Acknowledgment ack;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private KafkaConsumer kafkaConsumer;

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
  @DisplayName("FollowerDecreaseEvent 발생 시 Kafka 메시지 전송 성공")
  void onFollowerDecreaseEvent_shouldSendKafkaMessageWithHeaders() throws Exception {
    // given
    UUID followId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowerDecreaseEvent event = new FollowerDecreaseEvent(followId, followeeId);

    String expectedJson = "{\"event\":\"decrease\"}";
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
    assertThat(record.topic()).isEqualTo("mopl-follower-decrease");
    assertThat(record.key()).isEqualTo(followeeId.toString());
    assertThat(record.value()).isEqualTo(expectedJson);

    Header typeHeader = record.headers().lastHeader("x-event-type");
    assertThat(typeHeader).isNotNull();
    assertThat(new String(typeHeader.value(), StandardCharsets.UTF_8))
            .isEqualTo(event.getClass().getSimpleName());
  }

  @Test
  @DisplayName("JSON 직렬화 실패 시 카프카 메시지 전송이 수행되지 않음")
  void send_shouldNotCallKafka_whenJsonSerializationFails() throws Exception {
    // given
    UUID followId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowerIncreaseEvent event = new FollowerIncreaseEvent(followId, followeeId);

    when(objectMapper.writeValueAsString(event))
            .thenThrow(new JsonProcessingException("serialize error") {});

    // when
    kafkaEventListener.on(event);

    // then
    verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
  }

  @Test
  @DisplayName("카프카 메시지 전송 실패 시 whenComplete 예외 로그 흐름 수행 테스트")
  void send_shouldHandleKafkaSendFailure() throws Exception {
    // given
    UUID followId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowerIncreaseEvent event = new FollowerIncreaseEvent(followId, followeeId);
    when(objectMapper.writeValueAsString(event)).thenReturn("{}");

    CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

    // when
    kafkaEventListener.on(event);

    // future 실패 강제 발생
    Exception ex = new RuntimeException("SEND_FAIL");
    future.completeExceptionally(ex);

    // then
    verify(kafkaTemplate).send(any(ProducerRecord.class));
    assertThat(future.isCompletedExceptionally()).isTrue();
  }

  @Test
  @DisplayName("DM 생성 이벤트가 처음 처리되는 경우: 알림 전송, ProcessedEvent 저장, ack 호출")
  void onDirectMessageCreated_firstTime_shouldSendNotificationAndSaveProcessedEventAndAck()
      throws Exception {

    // given
    String kafkaEventJson = "{\"test\":\"json\"}";

    DirectMessageDto dto = mock(DirectMessageDto.class);
    UUID dmId = UUID.randomUUID();
    when(dto.id()).thenReturn(dmId);

    DirectMessageCreateEvent event = mock(DirectMessageCreateEvent.class);
    when(event.directMessageDto()).thenReturn(dto);

    when(objectMapper.readValue(kafkaEventJson, DirectMessageCreateEvent.class))
        .thenReturn(event);

    when(processedEventRepository.findByEventIdAndEventType(dmId, EventType.DIRECT_MESSAGE_CREATED))
        .thenReturn(Optional.empty());

    // when
    kafkaConsumer.onDirectMessageCreated(kafkaEventJson, ack);

    // then
    // 1) 이미 처리 여부 조회
    verify(processedEventRepository, times(1))
        .findByEventIdAndEventType(dmId, EventType.DIRECT_MESSAGE_CREATED);

    // 2) 알림 서비스 호출
    verify(notificationService, times(1)).sendDirectMessage(dto);

    // 3) ProcessedEvent 저장 내용 검증
    ArgumentCaptor<ProcessedEvent> processedEventCaptor =
        ArgumentCaptor.forClass(ProcessedEvent.class);

    verify(processedEventRepository, times(1))
        .save(processedEventCaptor.capture());

    ProcessedEvent saved = processedEventCaptor.getValue();
    assertThat(saved.getEventId()).isEqualTo(dmId);
    assertThat(saved.getEventType()).isEqualTo(EventType.DIRECT_MESSAGE_CREATED);

    // 4) ack 호출
    verify(ack, times(1)).acknowledge();
  }

  @Test
  @DisplayName("이미 처리된 DM 생성 이벤트이면 다시 처리하지 않고 ack만 호출")
  void onDirectMessageCreated_alreadyProcessed_shouldOnlyAck() throws Exception {
    // given
    String kafkaEventJson = "{\"test\":\"json\"}";

    DirectMessageDto dto = mock(DirectMessageDto.class);
    UUID dmId = UUID.randomUUID();
    when(dto.id()).thenReturn(dmId);

    DirectMessageCreateEvent event = mock(DirectMessageCreateEvent.class);
    when(event.directMessageDto()).thenReturn(dto);

    when(objectMapper.readValue(kafkaEventJson, DirectMessageCreateEvent.class))
        .thenReturn(event);

    ProcessedEvent existing = new ProcessedEvent(dmId, EventType.DIRECT_MESSAGE_CREATED);
    when(processedEventRepository.findByEventIdAndEventType(dmId, EventType.DIRECT_MESSAGE_CREATED))
        .thenReturn(Optional.of(existing));

    // when
    kafkaConsumer.onDirectMessageCreated(kafkaEventJson, ack);

    // then
    // 알림 및 save 는 호출되지 않는다
    verify(notificationService, never()).sendDirectMessage(any());
    verify(processedEventRepository, never()).save(any());

    // ack 는 호출됨
    verify(ack, times(1)).acknowledge();
  }
}
