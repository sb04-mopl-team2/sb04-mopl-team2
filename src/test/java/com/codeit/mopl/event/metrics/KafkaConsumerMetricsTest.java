package com.codeit.mopl.event.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.consumer.KafkaConsumer;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.event.PlayListCreateEvent;
import com.codeit.mopl.event.event.UserRoleUpdateEvent;
import com.codeit.mopl.event.metrics.KafkaEventStats10mCollector;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.codeit.mopl.mail.service.MailService;
import com.codeit.mopl.mail.utils.RedisStoreUtils;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.codeit.mopl.sse.service.SseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.support.Acknowledgment;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerMetricsTest {

  @Mock ObjectMapper objectMapper;
  @Mock NotificationService notificationService;
  @Mock ProcessedEventRepository processedEventRepository;
  @Mock SseService sseService;
  @Mock SseEmitterRegistry sseEmitterRegistry;
  @Mock MailService mailService;
  @Mock RedisStoreUtils redisStoreUtils;
  @Mock KafkaEventStats10mCollector statsCollector;
  @Mock Acknowledgment ack;

  KafkaConsumer kafkaConsumer;

  @BeforeEach
  void setUp() {
    kafkaConsumer = new KafkaConsumer(
        objectMapper,
        notificationService,
        processedEventRepository,
        sseService,
        sseEmitterRegistry,
        mailService,
        redisStoreUtils,
        statsCollector
    );
  }

  // -----------------------
  // 1) onUserRoleUpdated
  // -----------------------

  @Test
  void onUserRoleUpdated_whenDuplicate_shouldIncTotalAndIncDup_andAck() throws Exception {
    // given
    String json = "ANY";
    UUID eventId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    UserRoleUpdateEvent event = new UserRoleUpdateEvent(eventId, userId, /*before*/ null, /*after*/ null);
    // ↑ Role enum이 들어간다면 Role.USER, Role.ADMIN 등으로 채워줘

    when(objectMapper.readValue(eq(json), eq(UserRoleUpdateEvent.class))).thenReturn(event);

    when(processedEventRepository.findByEventIdAndEventType(eq(eventId), eq(EventType.NOTIFICATION_CREATE)))
        .thenReturn(Optional.of(new ProcessedEvent(eventId, EventType.NOTIFICATION_CREATE)));

    // when
    kafkaConsumer.onUserRoleUpdated(json, ack);

    // then: 지표
    verify(statsCollector).incTotal(any()); // USER_ROLE_UPDATE 키
    verify(statsCollector).incDup(any());
    verify(statsCollector, never()).incFail(any());

    // then: 흐름
    verify(ack).acknowledge();
    verify(notificationService, never()).createNotification(any(), anyString(), anyString(), any(Level.class));
  }

  @Test
  void onUserRoleUpdated_whenJsonFail_shouldIncTotalAndIncFail_andAck() throws Exception {
    // given
    String invalid = "INVALID_JSON";
    doThrow(new JsonProcessingException("fail") {})
        .when(objectMapper)
        .readValue(eq(invalid), eq(UserRoleUpdateEvent.class));

    // when
    kafkaConsumer.onUserRoleUpdated(invalid, ack);

    // then
    verify(statsCollector).incTotal(any());
    verify(statsCollector).incFail(any());
    verify(statsCollector, never()).incDup(any());

    verify(processedEventRepository, never()).findByEventIdAndEventType(any(), any());
    verify(ack).acknowledge();
  }

  @Test
  void onUserRoleUpdated_whenUnexpectedException_shouldIncTotalAndIncFail_andThrow() throws Exception {
    // given
    String json = "ANY";
    doThrow(new RuntimeException("boom"))
        .when(objectMapper)
        .readValue(eq(json), eq(UserRoleUpdateEvent.class));

    // when + then
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> kafkaConsumer.onUserRoleUpdated(json, ack))
        .isInstanceOf(RuntimeException.class);

    verify(statsCollector).incTotal(any());
    verify(statsCollector).incFail(any());
    // 여기선 catch(Exception)에서 throw e 하므로 ack는 호출되지 않음이 정상(현재 코드 기준)
    verify(ack, never()).acknowledge();
  }

  // -----------------------
  // 2) onPlayListCreated
  // -----------------------

  @Test
  void onPlayListCreated_whenDuplicate_shouldIncTotalAndIncDup_andAck() throws Exception {
    // given
    String json = "ANY";
    UUID playlistId = UUID.randomUUID();

    // PlayListCreateEvent 실제 생성자에 맞게 수정
    PlayListCreateEvent event = mock(PlayListCreateEvent.class);
    when(event.playListId()).thenReturn(playlistId);

    when(objectMapper.readValue(eq(json), eq(PlayListCreateEvent.class))).thenReturn(event);

    when(processedEventRepository.findByEventIdAndEventType(eq(playlistId), eq(EventType.PLAY_LIST_CREATED)))
        .thenReturn(Optional.of(new ProcessedEvent(playlistId, EventType.PLAY_LIST_CREATED)));

    // when
    kafkaConsumer.onPlayListCreated(json, ack);

    // then
    verify(statsCollector).incTotal(any());
    verify(statsCollector).incDup(any());
    verify(statsCollector, never()).incFail(any());
    verify(ack).acknowledge();

    verify(notificationService, never()).notifyFollowersOnPlaylistCreated(any());
  }

  @Test
  void onPlayListCreated_whenJsonFail_shouldIncTotalAndIncFail_andAck() throws Exception {
    // given
    String invalid = "INVALID_JSON";
    doThrow(new JsonProcessingException("fail") {})
        .when(objectMapper)
        .readValue(eq(invalid), eq(PlayListCreateEvent.class));

    // when
    kafkaConsumer.onPlayListCreated(invalid, ack);

    // then
    verify(statsCollector).incTotal(any());
    verify(statsCollector).incFail(any());
    verify(statsCollector, never()).incDup(any());
    verify(ack).acknowledge();
  }

  // -----------------------
  // 3) onNotificationCreated
  // -----------------------

  @Test
  void onNotificationCreated_whenDuplicate_shouldIncTotalAndIncDup_andAck() throws Exception {
    // given
    String json = "ANY";
    UUID notificationId = UUID.randomUUID();

    // NotificationDto / NotificationCreateEvent 생성이 프로젝트마다 다를 수 있어서 mock으로 처리
    NotificationDto dto = mock(NotificationDto.class);
    when(dto.id()).thenReturn(notificationId);

    NotificationCreateEvent event = mock(NotificationCreateEvent.class);
    when(event.notificationDto()).thenReturn(dto);

    when(objectMapper.readValue(eq(json), eq(NotificationCreateEvent.class))).thenReturn(event);

    when(processedEventRepository.findByEventIdAndEventType(eq(notificationId), eq(EventType.NOTIFICATION_CREATED)))
        .thenReturn(Optional.of(new ProcessedEvent(notificationId, EventType.NOTIFICATION_CREATED)));

    // when
    kafkaConsumer.onNotificationCreated(json, ack);

    // then
    verify(statsCollector).incTotal(any());
    verify(statsCollector).incDup(any());
    verify(statsCollector, never()).incFail(any());
    verify(ack).acknowledge();

    verify(notificationService, never()).sendNotification(any());
  }

  @Test
  void onNotificationCreated_whenJsonFail_shouldIncTotalAndIncFail_andAck() throws Exception {
    // given
    String invalid = "INVALID_JSON";
    doThrow(new JsonProcessingException("fail") {})
        .when(objectMapper)
        .readValue(eq(invalid), eq(NotificationCreateEvent.class));

    // when
    kafkaConsumer.onNotificationCreated(invalid, ack);

    // then
    verify(statsCollector).incTotal(any());
    verify(statsCollector).incFail(any());
    verify(statsCollector, never()).incDup(any());
    verify(ack).acknowledge();
  }
}
