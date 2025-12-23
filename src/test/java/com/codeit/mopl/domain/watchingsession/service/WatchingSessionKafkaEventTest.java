package com.codeit.mopl.domain.watchingsession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.mapper.WatchingSessionMapper;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.mopl.event.event.WatchingSessionCreateEvent;
import com.codeit.mopl.search.document.ContentDocument;
import com.codeit.mopl.search.repository.ContentOsRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class WatchingSessionKafkaEventTest {

  @Mock
  private WatchingSessionRepository watchingSessionRepository;

  @Mock
  private WatchingSessionMapper watchingSessionMapper;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private ContentOsRepository osRepository;

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  private WatchingSessionService watchingSessionService;

  @BeforeEach
  void setUp() {
    watchingSessionService = new WatchingSessionService(
        watchingSessionRepository,
        watchingSessionMapper,
        contentRepository,
        userRepository,
        eventPublisher,
        redisTemplate
    );
  }

  @Test
  @DisplayName("ensureSessionExists - 기존 세션이 없으면 새 세션 생성 후 WatchingSessionCreateEvent 발행")
  void ensureSessionExists_newSession_publishesWatchingSessionCreateEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    UUID watchingSessionId = UUID.randomUUID();

    // content 조회 성공
    Content content = mock(Content.class);
    when(contentRepository.findById(contentId))
        .thenReturn(Optional.of(content));
    when(content.getTitle()).thenReturn("테스트 콘텐츠");
    when(content.getTags()).thenReturn(List.of());
    ContentDocument contentDocument = mock(ContentDocument.class);
    when(osRepository.findById(contentId.toString()))
        .thenReturn(Optional.of(contentDocument));

    // user 조회 성공
    User user = mock(User.class);
    when(userRepository.findById(userId))
        .thenReturn(Optional.of(user));

    // 기존 세션 없음
    when(watchingSessionRepository.findByUserIdAndContentId(userId, contentId))
        .thenReturn(Optional.empty());

    // save 시 반환될 엔티티
    WatchingSession savedSession = mock(WatchingSession.class);
    when(savedSession.getId()).thenReturn(watchingSessionId);
    when(savedSession.getContent()).thenReturn(content);

    when(watchingSessionRepository.save(any(WatchingSession.class)))
        .thenReturn(savedSession);

    // when
    WatchingSession result = watchingSessionService.ensureSessionExists(userId, contentId);

    // then
    assertThat(result).isSameAs(savedSession);

    ArgumentCaptor<WatchingSessionCreateEvent> eventCaptor =
        ArgumentCaptor.forClass(WatchingSessionCreateEvent.class);

    verify(eventPublisher, times(1))
        .publishEvent(eventCaptor.capture());

    WatchingSessionCreateEvent event = eventCaptor.getValue();

    assertThat(event.watchingSessionId()).isEqualTo(watchingSessionId);
    assertThat(event.watchingSessionContentTitle()).isEqualTo("테스트 콘텐츠");
    verify(watchingSessionRepository, times(1)).deleteByUserId(userId);
    verify(watchingSessionRepository, times(1)).flush();
    verify(watchingSessionRepository, times(1)).save(any(WatchingSession.class));
  }
}
