package com.codeit.mopl.event;

import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.event.consumer.KafkaConsumer;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.DirectMessageCreateEvent;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.event.event.PlayListCreateEvent;
import com.codeit.mopl.event.event.WatchingSessionCreateEvent;
import com.codeit.mopl.event.listener.KafkaEventListener;
import com.codeit.mopl.event.metrics.KafkaEventStats10mCollector;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.codeit.mopl.event.sender.KafkaEventSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    private KafkaEventListener kafkaEventListener;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private Acknowledgment ack;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DirectMessageCreateEvent directMessageCreateEvent;

    @Mock
    private DirectMessageDto directMessageDto;

    @Mock
    private PlayListCreateEvent playListCreateEvent;

    @Mock
    private WatchingSessionCreateEvent watchingSessionCreateEvent;

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    @Mock
    private KafkaEventStats10mCollector statsCollector;

    @Mock
    private KafkaEventSender sender;

    @BeforeEach
    void setUp() {
        kafkaEventListener = new KafkaEventListener(sender);
        MDC.clear();
    }

    @Test
    @DisplayName("NotificationCreateEvent 발생 시 KafkaEventSender로 위임")
    void onNotificationCreateEvent_shouldDelegateToSender() {
        // given
        UUID id = UUID.randomUUID();
        NotificationDto dto = mock(NotificationDto.class);
        when(dto.id()).thenReturn(id);

        NotificationCreateEvent event = mock(NotificationCreateEvent.class);
        when(event.notificationDto()).thenReturn(dto);

        // when
        kafkaEventListener.on(event);

        // then
        verify(sender, times(1)).send(
                eq("mopl-notification-create"),
                eq(id.toString()),
                eq(event)
        );
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

    @Test
    @DisplayName("DM 이벤트 - id 가 있으면 id.toString() 을 key 로, KafkaEventSender에 위임한다.")
    void onDirectMessageCreateEvent_withId() {
        // given
        UUID dmId = UUID.randomUUID();

        when(directMessageCreateEvent.directMessageDto())
                .thenReturn(directMessageDto);
        when(directMessageDto.id())
                .thenReturn(dmId);

        // when
        kafkaEventListener.on(directMessageCreateEvent);

        // then
        verify(sender, times(1)).send(
                eq("mopl-directMessage-create"),
                eq(dmId.toString()),
                eq(directMessageCreateEvent)
        );
    }

    @Test
    @DisplayName("DM 이벤트 - id 가 null 이면 key 를 null 로, KafkaEventSender에 위임한다.")
    void onDirectMessageCreateEvent_withoutId() {
        // given
        when(directMessageCreateEvent.directMessageDto())
                .thenReturn(directMessageDto);
        when(directMessageDto.id())
                .thenReturn(null);

        // when
        kafkaEventListener.on(directMessageCreateEvent);

        // then
        verify(sender, times(1)).send(
                eq("mopl-directMessage-create"),
                eq(null),
                eq(directMessageCreateEvent)
        );
    }

    @Test
    @DisplayName("플레이리스트 이벤트 - playListId 가 있으면 id.toString() 을 key 로 KafkaEventSender에 위임한다.")
    void onPlayListCreateEvent_withId() {
        // given
        UUID playlistId = UUID.randomUUID();

        when(playListCreateEvent.playListId())
                .thenReturn(playlistId);

        // when
        kafkaEventListener.on(playListCreateEvent);

        // then
        verify(sender, times(1)).send(
                eq("mopl-playList-create"),
                eq(playlistId.toString()),
                eq(playListCreateEvent)
        );
    }

    @Test
    @DisplayName("플레이리스트 이벤트 - playListId 가 null 이면 key 에 null 로 KafkaEventSender에 위임한다.")
    void onPlayListCreateEvent_withoutId() {
        // given
        when(playListCreateEvent.playListId())
                .thenReturn(null);

        // when
        kafkaEventListener.on(playListCreateEvent);

        // then
        verify(sender, times(1)).send(
                eq("mopl-playList-create"),
                eq(null),
                eq(playListCreateEvent)
        );
    }

    @Test
    @DisplayName("WatchingSession 이벤트 - watchingSessionId 가 있으면 id.toString() 을 key 로 KafkaEventSender에 위임한다.")
    void onWatchingSessionCreateEvent_withId() {
        // given
        UUID watchingSessionId = UUID.randomUUID();

        when(watchingSessionCreateEvent.watchingSessionId())
                .thenReturn(watchingSessionId);

        // when
        kafkaEventListener.on(watchingSessionCreateEvent);

        // then
        verify(sender, times(1)).send(
                eq("mopl-watchingSession-create"),
                eq(watchingSessionId.toString()),
                eq(watchingSessionCreateEvent)
        );
    }

    @Test
    @DisplayName("WatchingSession 이벤트 - watchingSessionId 가 null 이면 key 에 null 로 KafkaEventSender에 위임한다.")
    void onWatchingSessionCreateEvent_withoutId() {
        // given
        when(watchingSessionCreateEvent.watchingSessionId())
                .thenReturn(null);

        // when
        kafkaEventListener.on(watchingSessionCreateEvent);

        // then
        verify(sender, times(1)).send(
                eq("mopl-watchingSession-create"),
                eq(null),
                eq(watchingSessionCreateEvent)
        );
    }

    @Test
    @DisplayName("플레이리스트 생성 이벤트 처음 처리: 팔로워 알림 전송, ProcessedEvent 저장, ack 호출")
    void onPlayListCreated_firstTime_shouldNotifyFollowersAndSaveProcessedEventAndAck() throws Exception {
        // given
        String kafkaEventJson = "{\"test\":\"json\"}";
        UUID playlistId = UUID.randomUUID();

        PlayListCreateEvent event = mock(PlayListCreateEvent.class);
        when(event.playListId()).thenReturn(playlistId);

        when(objectMapper.readValue(kafkaEventJson, PlayListCreateEvent.class))
                .thenReturn(event);

        // 아직 처리되지 않은 이벤트
        when(processedEventRepository.findByEventIdAndEventType(
                playlistId, EventType.PLAY_LIST_CREATED))
                .thenReturn(Optional.empty());

        // when
        kafkaConsumer.onPlayListCreated(kafkaEventJson, ack);

        // then
        // 1) idempotency 조회
        verify(processedEventRepository, times(1))
                .findByEventIdAndEventType(playlistId, EventType.PLAY_LIST_CREATED);

        // 2) 팔로워 알림 위임
        verify(notificationService, times(1))
                .notifyFollowersOnPlaylistCreated(event);

        // 3) ProcessedEvent 저장
        verify(processedEventRepository, times(1))
                .save(any(ProcessedEvent.class));

        // 4) ack 호출
        verify(ack, times(1)).acknowledge();
    }

    @Test
    @DisplayName("시청 세션 시작 이벤트 처음 처리: 팔로워 알림 전송, ProcessedEvent 저장, ack 호출")
    void onWatchingSessionCreated_firstTime_shouldNotifyFollowersAndSaveProcessedEventAndAck() throws Exception {
        // given
        String kafkaEventJson = "{\"test\":\"json\"}";
        UUID watchingSessionId = UUID.randomUUID();

        WatchingSessionCreateEvent event = mock(WatchingSessionCreateEvent.class);
        when(event.watchingSessionId()).thenReturn(watchingSessionId);

        when(objectMapper.readValue(kafkaEventJson, WatchingSessionCreateEvent.class))
                .thenReturn(event);

        // 아직 처리되지 않은 이벤트
        when(processedEventRepository.findByEventIdAndEventType(
                watchingSessionId, EventType.WATCH_SESSION_CREATED))
                .thenReturn(Optional.empty());

        // when
        kafkaConsumer.onWatchingSessionCreated(kafkaEventJson, ack);

        // then
        // 1) idempotency 조회
        verify(processedEventRepository, times(1))
                .findByEventIdAndEventType(watchingSessionId, EventType.WATCH_SESSION_CREATED);

        // 2) 팔로워 알림 위임
        verify(notificationService, times(1))
                .notifyFollowersOnWatchingEvent(event);

        // 3) ProcessedEvent 저장
        verify(processedEventRepository, times(1))
                .save(any(ProcessedEvent.class));

        // 4) ack 호출
        verify(ack, times(1)).acknowledge();
    }

    @Test
    @DisplayName("이미 처리된 플레이리스트 생성 이벤트이면 다시 처리하지 않고 ack만 호출")
    void onPlayListCreated_alreadyProcessed_shouldOnlyAck() throws Exception {
        // given
        String kafkaEventJson = "{\"test\":\"json\"}";
        UUID playlistId = UUID.randomUUID();

        PlayListCreateEvent event = mock(PlayListCreateEvent.class);
        when(event.playListId()).thenReturn(playlistId);

        when(objectMapper.readValue(kafkaEventJson, PlayListCreateEvent.class))
                .thenReturn(event);

        ProcessedEvent existing = new ProcessedEvent(playlistId, EventType.PLAY_LIST_CREATED);
        when(processedEventRepository.findByEventIdAndEventType(playlistId, EventType.PLAY_LIST_CREATED))
                .thenReturn(Optional.of(existing));

        // when
        kafkaConsumer.onPlayListCreated(kafkaEventJson, ack);

        // then
        verify(notificationService, never()).notifyFollowersOnPlaylistCreated(any());
        verify(processedEventRepository, never()).save(any());
        verify(ack, times(1)).acknowledge();
    }

    @Test
    @DisplayName("이미 처리된 시청 세션 시작 이벤트이면 다시 처리하지 않고 ack만 호출")
    void onWatchingSessionCreated_alreadyProcessed_shouldOnlyAck() throws Exception {
        // given
        String kafkaEventJson = "{\"test\":\"json\"}";
        UUID watchingSessionId = UUID.randomUUID();

        WatchingSessionCreateEvent event = mock(WatchingSessionCreateEvent.class);
        when(event.watchingSessionId()).thenReturn(watchingSessionId);

        when(objectMapper.readValue(kafkaEventJson, WatchingSessionCreateEvent.class))
                .thenReturn(event);

        ProcessedEvent existing = new ProcessedEvent(watchingSessionId, EventType.WATCH_SESSION_CREATED);
        when(processedEventRepository.findByEventIdAndEventType(watchingSessionId, EventType.WATCH_SESSION_CREATED))
                .thenReturn(Optional.of(existing));

        // when
        kafkaConsumer.onWatchingSessionCreated(kafkaEventJson, ack);

        // then
        verify(notificationService, never()).notifyFollowersOnWatchingEvent(any());
        verify(processedEventRepository, never()).save(any());
        verify(ack, times(1)).acknowledge();
    }
}
