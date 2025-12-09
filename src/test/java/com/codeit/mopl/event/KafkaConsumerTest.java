package com.codeit.mopl.event;

import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.consumer.KafkaConsumer;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.*;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.codeit.mopl.mail.service.MailService;
import com.codeit.mopl.mail.utils.RedisStoreUtils;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.codeit.mopl.sse.service.SseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;
import java.util.UUID;

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

  @Mock
  private SseService sseService;

  @Mock
  private SseEmitterRegistry sseEmitterRegistry;

  private KafkaConsumer kafkaConsumer;

  @Mock
  private DirectMessageCreateEvent directMessageCreateEvent;

  @Mock
  private DirectMessageDto directMessageDto;

  @Mock
  private PlayListCreateEvent playListCreateEvent;

  @Mock
  private WatchingSessionCreateEvent watchingSessionCreateEvent;

  @Mock
  private RedisStoreUtils redisStoreUtils;

  @Mock
  private MailService mailService;

  @BeforeEach
  void setUp() {
    kafkaConsumer = new KafkaConsumer(objectMapper, notificationService, processedEventRepository, sseService, sseEmitterRegistry, mailService, redisStoreUtils);
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

  @Test
  @DisplayName("DM 생성 - 아직 처리되지 않은 이벤트면 sendDirectMessage 호출 후 processedEvent 저장, ack 호출")
  void onDirectMessageCreated_success() throws Exception {
    // given
    String json = "{\"id\":\"dm\"}";
    UUID eventId = UUID.randomUUID();

    when(objectMapper.readValue(json, DirectMessageCreateEvent.class))
        .thenReturn(directMessageCreateEvent);
    when(directMessageCreateEvent.directMessageDto())
        .thenReturn(directMessageDto);
    when(directMessageDto.id())
        .thenReturn(eventId);

    when(processedEventRepository.findByEventIdAndEventType(eventId, EventType.DIRECT_MESSAGE_CREATED))
        .thenReturn(Optional.empty());

    // when
    kafkaConsumer.onDirectMessageCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(eventId, EventType.DIRECT_MESSAGE_CREATED);
    verify(notificationService).sendDirectMessage(directMessageDto);
    verify(processedEventRepository).save(any(ProcessedEvent.class));
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("DM 생성 - 이미 처리된 이벤트면 sendDirectMessage 호출 안 하고 ack만 호출")
  void onDirectMessageCreated_idempotent() throws Exception {
    // given
    String json = "{\"id\":\"dm\"}";
    UUID eventId = UUID.randomUUID();

    when(objectMapper.readValue(json, DirectMessageCreateEvent.class))
        .thenReturn(directMessageCreateEvent);
    when(directMessageCreateEvent.directMessageDto())
        .thenReturn(directMessageDto);
    when(directMessageDto.id())
        .thenReturn(eventId);

    when(processedEventRepository.findByEventIdAndEventType(eventId, EventType.DIRECT_MESSAGE_CREATED))
        .thenReturn(Optional.of(new ProcessedEvent(eventId, EventType.DIRECT_MESSAGE_CREATED)));

    // when
    kafkaConsumer.onDirectMessageCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(eventId, EventType.DIRECT_MESSAGE_CREATED);
    verify(notificationService, never()).sendDirectMessage(any());
    verify(processedEventRepository, never()).save(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("DM 생성 - JSON 역직렬화 실패 시 repository, service 호출 없이 ack만 호출")
  void onDirectMessageCreated_jsonDeserializeFail() throws Exception {
    // given
    String invalidJson = "INVALID_JSON";

    when(objectMapper.readValue(invalidJson, DirectMessageCreateEvent.class))
        .thenThrow(new JsonProcessingException("fail") {});

    // when
    kafkaConsumer.onDirectMessageCreated(invalidJson, ack);

    // then
    verify(processedEventRepository, never()).findByEventIdAndEventType(any(), any());
    verify(processedEventRepository, never()).save(any());
    verify(notificationService, never()).sendDirectMessage(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("DM 생성 - 기타 예외 발생 시 ack 호출 없이 예외 전파")
  void onDirectMessageCreated_unexpectedException() throws Exception {
    // given
    String json = "{\"id\":\"dm\"}";
    UUID eventId = UUID.randomUUID();

    when(objectMapper.readValue(json, DirectMessageCreateEvent.class))
        .thenReturn(directMessageCreateEvent);
    when(directMessageCreateEvent.directMessageDto())
        .thenReturn(directMessageDto);
    when(directMessageDto.id())
        .thenReturn(eventId);

    when(processedEventRepository.findByEventIdAndEventType(eventId, EventType.DIRECT_MESSAGE_CREATED))
        .thenReturn(Optional.empty());

    doThrow(new RuntimeException("unexpected"))
        .when(notificationService).sendDirectMessage(directMessageDto);

    // when & then
    assertThatThrownBy(() -> kafkaConsumer.onDirectMessageCreated(json, ack))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("unexpected");

    verify(ack, never()).acknowledge();
  }

  @Test
  @DisplayName("플레이리스트 생성 - 아직 처리되지 않은 이벤트면 notifyFollowersOnPlaylistCreated 호출, processedEvent 저장, ack 호출")
  void onPlayListCreated_success() throws Exception {
    // given
    String json = "{\"id\":\"playlist\"}";
    UUID playListId = UUID.randomUUID();

    when(objectMapper.readValue(json, PlayListCreateEvent.class))
        .thenReturn(playListCreateEvent);
    when(playListCreateEvent.playListId())
        .thenReturn(playListId);

    when(processedEventRepository.findByEventIdAndEventType(playListId, EventType.PLAY_LIST_CREATED))
        .thenReturn(Optional.empty());

    // when
    kafkaConsumer.onPlayListCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(playListId, EventType.PLAY_LIST_CREATED);
    verify(notificationService).notifyFollowersOnPlaylistCreated(playListCreateEvent);
    verify(processedEventRepository).save(any(ProcessedEvent.class));
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("플레이리스트 생성 - 이미 처리된 이벤트면 followService 호출 안 하고 ack만 호출")
  void onPlayListCreated_idempotent() throws Exception {
    // given
    String json = "{\"id\":\"playlist\"}";
    UUID playListId = UUID.randomUUID();

    when(objectMapper.readValue(json, PlayListCreateEvent.class))
        .thenReturn(playListCreateEvent);
    when(playListCreateEvent.playListId())
        .thenReturn(playListId);

    when(processedEventRepository.findByEventIdAndEventType(playListId, EventType.PLAY_LIST_CREATED))
        .thenReturn(Optional.of(new ProcessedEvent(playListId, EventType.PLAY_LIST_CREATED)));

    // when
    kafkaConsumer.onPlayListCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(playListId, EventType.PLAY_LIST_CREATED);
    verify(notificationService, never()).notifyFollowersOnPlaylistCreated(any());
    verify(processedEventRepository, never()).save(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("플레이리스트 생성 - JSON 역직렬화 실패 시 followService, repository 호출 없이 ack만 호출")
  void onPlayListCreated_jsonDeserializeFail() throws Exception {
    // given
    String invalidJson = "INVALID_JSON";

    when(objectMapper.readValue(invalidJson, PlayListCreateEvent.class))
        .thenThrow(new JsonProcessingException("fail") {});

    // when
    kafkaConsumer.onPlayListCreated(invalidJson, ack);

    // then
    verify(processedEventRepository, never()).findByEventIdAndEventType(any(), any());
    verify(processedEventRepository, never()).save(any());
    verify(notificationService, never()).notifyFollowersOnPlaylistCreated(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("플레이리스트 생성 - 기타 예외 발생 시 ack 호출 없이 예외 전파")
  void onPlayListCreated_unexpectedException() throws Exception {
    // given
    String json = "{\"id\":\"playlist\"}";
    UUID playListId = UUID.randomUUID();

    when(objectMapper.readValue(json, PlayListCreateEvent.class))
        .thenReturn(playListCreateEvent);
    when(playListCreateEvent.playListId())
        .thenReturn(playListId);

    when(processedEventRepository.findByEventIdAndEventType(playListId, EventType.PLAY_LIST_CREATED))
        .thenReturn(Optional.empty());

    doThrow(new RuntimeException("unexpected"))
        .when(notificationService).notifyFollowersOnPlaylistCreated(playListCreateEvent);

    // when & then
    assertThatThrownBy(() -> kafkaConsumer.onPlayListCreated(json, ack))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("unexpected");

    verify(ack, never()).acknowledge();
  }

  @Test
  @DisplayName("WatchingSession 생성 - 아직 처리되지 않은 이벤트면 notifyFollowersOnWatchingEvent 호출, processedEvent 저장, ack 호출")
  void onWatchingSessionCreated_success() throws Exception {
    // given
    String json = "{\"id\":\"watchingSession\"}";
    UUID watchingSessionId = UUID.randomUUID();

    when(objectMapper.readValue(json, WatchingSessionCreateEvent.class))
        .thenReturn(watchingSessionCreateEvent);
    when(watchingSessionCreateEvent.watchingSessionId())
        .thenReturn(watchingSessionId);

    when(processedEventRepository.findByEventIdAndEventType(watchingSessionId, EventType.WATCH_SESSION_CREATED))
        .thenReturn(Optional.empty());

    // when
    kafkaConsumer.onWatchingSessionCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(watchingSessionId, EventType.WATCH_SESSION_CREATED);
    verify(notificationService).notifyFollowersOnWatchingEvent(watchingSessionCreateEvent);
    verify(processedEventRepository).save(any(ProcessedEvent.class));
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("WatchingSession 생성 - 이미 처리된 이벤트면 followService 호출 안 하고 ack만 호출")
  void onWatchingSessionCreated_idempotent() throws Exception {
    // given
    String json = "{\"id\":\"watchingSession\"}";
    UUID watchingSessionId = UUID.randomUUID();

    when(objectMapper.readValue(json, WatchingSessionCreateEvent.class))
        .thenReturn(watchingSessionCreateEvent);
    when(watchingSessionCreateEvent.watchingSessionId())
        .thenReturn(watchingSessionId);

    when(processedEventRepository.findByEventIdAndEventType(watchingSessionId, EventType.WATCH_SESSION_CREATED))
        .thenReturn(Optional.of(new ProcessedEvent(watchingSessionId, EventType.WATCH_SESSION_CREATED)));

    // when
    kafkaConsumer.onWatchingSessionCreated(json, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(watchingSessionId, EventType.WATCH_SESSION_CREATED);
    verify(notificationService, never()).notifyFollowersOnWatchingEvent(any());
    verify(processedEventRepository, never()).save(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("WatchingSession 생성 - JSON 역직렬화 실패 시 followService, repository 호출 없이 ack만 호출")
  void onWatchingSessionCreated_jsonDeserializeFail() throws Exception {
    // given
    String invalidJson = "INVALID_JSON";

    when(objectMapper.readValue(invalidJson, WatchingSessionCreateEvent.class))
        .thenThrow(new JsonProcessingException("fail") {});

    // when
    kafkaConsumer.onWatchingSessionCreated(invalidJson, ack);

    // then
    verify(processedEventRepository, never()).findByEventIdAndEventType(any(), any());
    verify(processedEventRepository, never()).save(any());
    verify(notificationService, never()).notifyFollowersOnWatchingEvent(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("WatchingSession 생성 - 기타 예외 발생 시 ack 호출 없이 예외 전파")
  void onWatchingSessionCreated_unexpectedException() throws Exception {
    // given
    String json = "{\"id\":\"watchingSession\"}";
    UUID watchingSessionId = UUID.randomUUID();

    when(objectMapper.readValue(json, WatchingSessionCreateEvent.class))
        .thenReturn(watchingSessionCreateEvent);
    when(watchingSessionCreateEvent.watchingSessionId())
        .thenReturn(watchingSessionId);

    when(processedEventRepository.findByEventIdAndEventType(watchingSessionId, EventType.WATCH_SESSION_CREATED))
        .thenReturn(Optional.empty());

    doThrow(new RuntimeException("unexpected"))
        .when(notificationService).notifyFollowersOnWatchingEvent(watchingSessionCreateEvent);

    // when & then
    assertThatThrownBy(() -> kafkaConsumer.onWatchingSessionCreated(json, ack))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("unexpected");

    verify(ack, never()).acknowledge();
  }

  @Test
  @DisplayName("메일 발송 - 아직 처리되지 않은 이벤트면 Redis 저장 및 메일 발송, processedEvent 저장, ack 호출")
  void mailSend_success() throws Exception {
    // given
    String kafkaEventJson = "\"eventId\"=\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"";
    MailSendEvent event = new MailSendEvent(
            UUID.randomUUID(),
            "test@example.com",
            "tempPw"
    );

    when(objectMapper.readValue(kafkaEventJson, MailSendEvent.class))
            .thenReturn(event);
    when(processedEventRepository.findByEventIdAndEventType(event.eventId(), EventType.MAIL_SEND))
            .thenReturn(Optional.empty());

    // when
    kafkaConsumer.onMailSend(kafkaEventJson, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(event.eventId(), EventType.MAIL_SEND);
    verify(redisStoreUtils).storeTempPassword(anyString(),anyString());
    verify(mailService).sendMail(anyString(),anyString());
    verify(processedEventRepository).save(any(ProcessedEvent.class));
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("메일 발송 - 이미 처리된 이벤트면 이후 동작 안 하고 ack만 호출")
  void onMailSend_idempotent() throws Exception {
    // given
    String kafkaEventJson = "\"eventId\"=\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"";
    MailSendEvent event = new MailSendEvent(
            UUID.randomUUID(),
            "test@example.com",
            "tempPw"
    );

    when(objectMapper.readValue(kafkaEventJson, MailSendEvent.class))
            .thenReturn(event);

    when(processedEventRepository.findByEventIdAndEventType(event.eventId(), EventType.MAIL_SEND))
            .thenReturn(Optional.of(new ProcessedEvent(event.eventId(), EventType.MAIL_SEND)));

    // when
    kafkaConsumer.onMailSend(kafkaEventJson, ack);

    // then
    verify(processedEventRepository).findByEventIdAndEventType(event.eventId(), EventType.MAIL_SEND);
    verify(mailService, never()).sendMail(anyString(),anyString());
    verify(redisStoreUtils, never()).storeTempPassword(anyString(),anyString());
    verify(processedEventRepository, never()).save(any());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("메일 발송 - JSON 역직렬화 실패 시 이후 동작 없이 ack만 호출")
  void onMailSend_jsonDeserializeFail() throws Exception {
    // given
    String invalidJson = "INVALID_JSON";

    when(objectMapper.readValue(invalidJson, MailSendEvent.class))
            .thenThrow(new JsonProcessingException("fail") {});

    // when
    kafkaConsumer.onMailSend(invalidJson, ack);

    // then
    verify(processedEventRepository, never()).findByEventIdAndEventType(any(), any());
    verify(processedEventRepository, never()).save(any());
    verify(mailService, never()).sendMail(anyString(),anyString());
    verify(redisStoreUtils, never()).storeTempPassword(anyString(),anyString());
    verify(ack).acknowledge();
  }

  @Test
  @DisplayName("메일 발송 - 메일 발송 중 예외 발생 시 ack 호출 없이 예외 전파")
  void onMailSend_messagingException() throws Exception {
    // given
    String kafkaEventJson = "\"eventId\"=\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"";
    MailSendEvent event = new MailSendEvent(
            UUID.randomUUID(),
            "test@example.com",
            "tempPw"
    );

    when(objectMapper.readValue(kafkaEventJson, MailSendEvent.class))
            .thenReturn(event);

    when(processedEventRepository.findByEventIdAndEventType(event.eventId(), EventType.MAIL_SEND))
            .thenReturn(Optional.empty());

    doThrow(new MessagingException("unexpected"))
            .when(mailService).sendMail(anyString(),anyString());

    // when & then
    assertThatThrownBy(() -> kafkaConsumer.onMailSend(kafkaEventJson, ack))
            .isInstanceOf(MessagingException.class)
            .hasMessageContaining("unexpected");

    verify(ack, never()).acknowledge();
  }

  @Test
  @DisplayName("메일 발송 - 기타 예외 발생 시 ack 호출 없이 예외 전파")
  void onMailSend_unexpectedException() throws Exception {
    // given
    String kafkaEventJson = "\"eventId\"=\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"";
    MailSendEvent event = new MailSendEvent(
            UUID.randomUUID(),
            "test@example.com",
            "tempPw"
    );

    when(objectMapper.readValue(kafkaEventJson, MailSendEvent.class))
            .thenReturn(event);

    when(processedEventRepository.findByEventIdAndEventType(event.eventId(), EventType.MAIL_SEND))
            .thenReturn(Optional.empty());

    doThrow(new RuntimeException("unexpected"))
            .when(mailService).sendMail(anyString(),anyString());

    // when & then
    assertThatThrownBy(() -> kafkaConsumer.onMailSend(kafkaEventJson, ack))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("unexpected");

    verify(ack, never()).acknowledge();
  }
}
