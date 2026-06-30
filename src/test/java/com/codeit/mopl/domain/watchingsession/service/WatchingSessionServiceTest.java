package com.codeit.mopl.domain.watchingsession.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.base.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.domain.watchingsession.entity.enums.ChangeType;
import com.codeit.mopl.domain.watchingsession.mapper.WatchingSessionMapper;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.mopl.event.event.WatchingSessionCreateEvent;
import com.codeit.mopl.exception.content.ContentErrorCode;
import com.codeit.mopl.exception.content.ContentNotFoundException;
import com.codeit.mopl.exception.watchingsession.WatchingSessionErrorCode;
import com.codeit.mopl.exception.watchingsession.WatchingSessionNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class WatchingSessionServiceTest {

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
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private WatchingSessionService watchingSessionService;

  private User user;
  private Content content;
  private WatchingSession watchingSession;
  private UUID userId;
  private UUID contentId;
  private UUID watchingSessionId;

  @BeforeEach
  void init() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    user = new User("test@test.com", "pw", "test");
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    userId = user.getId();

    content = new Content();
    ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
    content.setContentType(ContentType.MOVIE);
    content.setTitle("testTitleContent");
    content.setDescription("");
    content.setTags(List.of());
    content.setAverageRating(0.0);
    content.setReviewCount(0);
    contentId = content.getId();

    watchingSession = new WatchingSession();
    ReflectionTestUtils.setField(watchingSession, "id", UUID.randomUUID());
    watchingSession.setUser(user);
    watchingSession.setContent(content);
    watchingSessionId = watchingSession.getId();
  }

  /*
    public WatchingSessionDto getByUserId(UUID userId)
   */
  @Test
  @DisplayName("UserId로 조회 성공")
  void getByUserIdSuccess() {
    // given
    UUID userId = UUID.randomUUID();
    when(watchingSessionRepository.findByUserId(userId)).thenReturn(Optional.of(watchingSession));
    WatchingSessionDto expectedDto = mock(WatchingSessionDto.class);
    when(watchingSessionMapper.toDto(watchingSession)).thenReturn(expectedDto);

    // when
    WatchingSessionDto result = watchingSessionService.getByUserId(userId);

    // then
    assertEquals(expectedDto, result);
    verify(watchingSessionMapper).toDto(watchingSession);
    verify(watchingSessionRepository).findByUserId(userId);
  }

  /*
     public CursorResponseWatchingSessionDto getWatchingSessions(..)
   */
  @Test
  @DisplayName("특정 콘텐츠의 시청 세션 목록 조회 성공")
  void getByContentIdSuccess() {
    // given
    UUID contentId = UUID.randomUUID();
    WatchingSessionDto watchingSessionDto = mock(WatchingSessionDto.class);
    when(watchingSessionRepository.findWatchingSessions(
        any(), any(), any(), any(),
        eq(11),
        any(), any()
    )).thenReturn(List.of(watchingSession));
    CursorResponseWatchingSessionDto expectedDto = new CursorResponseWatchingSessionDto(
        List.of(watchingSessionDto),
        null, null, false,
        1L, SortBy.CREATED_AT.getValue(), SortDirection.ASCENDING
    );
    when(contentRepository.existsById(contentId)).thenReturn(true);
    when(watchingSessionMapper.toDto(watchingSession)).thenReturn(watchingSessionDto);
    when(watchingSessionRepository.getWatcherCount(contentId, null)).thenReturn(1L);

    // when
    CursorResponseWatchingSessionDto result = watchingSessionService.getWatchingSessions(
        contentId,null, null, null,
        10, SortDirection.ASCENDING, SortBy.CREATED_AT);

    // then
    assertEquals(expectedDto, result);
    verify(watchingSessionRepository, times(1)).getWatcherCount(contentId, null);
    verify(watchingSessionMapper, times(1)).toDto(any());
  }

  @Test
  @DisplayName("존재하지 않는 ContentId면 실패")
  void getByNonExistentContentIdFailure() {
    // given
    UUID contentId = UUID.randomUUID();

    // when & then
    assertThrows(ContentNotFoundException.class, () -> {
      watchingSessionService.getWatchingSessions(
          contentId,
          null, null, null,
          10, SortDirection.ASCENDING, SortBy.CREATED_AT
      );
    });
    verify(contentRepository, times(1)).existsById(contentId);
    verify(watchingSessionRepository, times(0)).getWatcherCount(contentId, null);
    verify(watchingSessionMapper, times(0)).toDto(any());
  }

  @Test
  @DisplayName("hasNext를 가진 특정 콘텐츠의 시청 세션 목록 조회 성공")
  void getWithHasNextWatchingSessionSuccess() {
    // given
    UUID contentId = UUID.randomUUID();
    User user2 = new User("test2@test.com", "pw", "test2");
    // entity2
    WatchingSession entity2 = new WatchingSession();
    entity2.setUser(user2);
    entity2.setContent(content);
    UUID entity2Id = UUID.randomUUID();
    Instant entity2Time = Instant.now();
    ReflectionTestUtils.setField(entity2, "id", entity2Id);
    ReflectionTestUtils.setField(entity2, "createdAt", entity2Time);

    List<WatchingSession> mutableList = new ArrayList<>();
    mutableList.add(watchingSession);
    mutableList.add(entity2);

    when(watchingSessionRepository.findWatchingSessions(
        any(), any(), any(), any(),
        eq(2), // internal limit (1 + 1)
        any(), any()
    )).thenReturn(mutableList);

    WatchingSessionDto watchingSessionDto = mock(WatchingSessionDto.class);
    when(contentRepository.existsById(contentId)).thenReturn(true);
    when(watchingSessionMapper.toDto(watchingSession)).thenReturn(watchingSessionDto);
    when(watchingSessionRepository.getWatcherCount(contentId, null)).thenReturn(2L);

    // 예상 응답
    CursorResponseWatchingSessionDto expectedDto = new CursorResponseWatchingSessionDto(
        List.of(watchingSessionDto),
        entity2Time.toString(),
        entity2Id,
        true,
        2L, SortBy.CREATED_AT.getValue(), SortDirection.ASCENDING
    );

    // when
    CursorResponseWatchingSessionDto result = watchingSessionService.getWatchingSessions(
        contentId,null, null, null,
        1, SortDirection.ASCENDING, SortBy.CREATED_AT
    );

    // then
    assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
    verify(watchingSessionRepository, times(1)).getWatcherCount(
        contentId, null);
    verify(watchingSessionMapper, times(1)).toDto(any());
  }

  /*
    웹소켓용 이벤크 기반 함수들
 */
  @Test
  @DisplayName("joinSession - 세션이 이미 존재함")
  void joinSessionExistsSession() {
    // given
    when(contentRepository.findById(any(UUID.class))).thenReturn(Optional.of(content));
    when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));
    when(watchingSessionRepository.findByUserIdAndContentId(any(UUID.class), any(UUID.class))).thenReturn(Optional.of(watchingSession));

    // when
    WatchingSessionChange returnedWatchingSessionChange = watchingSessionService.joinSession(userId, contentId);
    WatchingSessionDto returnedWatchingSession = returnedWatchingSessionChange.watchingSession();

    // then
    assertThat(returnedWatchingSession.watcher().userId()).isEqualTo(userId);
    assertThat(returnedWatchingSession.content().id()).isEqualTo(contentId);
    verify(contentRepository).findById(contentId);
    verify(userRepository).findById(userId);
    verify(watchingSessionRepository).findByUserIdAndContentId(userId, contentId);
  }

  @Test
  @DisplayName("joinSession - 세션이 존재하지 않아 세션 새로 생성")
  void joinSessionNonExistentSession() {
    // given
    when(contentRepository.findById(any(UUID.class))).thenReturn(Optional.of(content));
    when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));
    when(watchingSessionRepository.findByUserIdAndContentId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());
    when(watchingSessionRepository.save(any(WatchingSession.class))).thenReturn(watchingSession);

    // when
    WatchingSessionChange returnedWatchingSessionChange = watchingSessionService.joinSession(userId,contentId);
    WatchingSessionDto returnedWatchingSession = returnedWatchingSessionChange.watchingSession();

    // then
    assertThat(returnedWatchingSession.watcher().userId()).isEqualTo(userId);
    assertThat(returnedWatchingSession.content().id()).isEqualTo(contentId);
    verify(eventPublisher).publishEvent(any(WatchingSessionCreateEvent.class));
  }

  @Test
  @DisplayName("joinSession - 컨텐츠가 존재하지 않아서 실패")
  void joinSessionNonExistentContentFailure() {
    // given
    when(contentRepository.findById(any(UUID.class)))
        .thenThrow(new ContentNotFoundException(
            ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId ))
        );

    // when & then
    assertThrows(ContentNotFoundException.class, () -> {
      watchingSessionService.joinSession(userId, contentId);
    });
  }

  @Test
  @DisplayName("leaveSession - 세션 제거 성공")
  void leaveSessionSuccess() {
    // given
    when(watchingSessionRepository.findById(any(UUID.class))).thenReturn(Optional.of(watchingSession));
    when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));

    // when
    WatchingSessionChange returnedWatchingSessionChange = watchingSessionService.leaveSession(
        userId, watchingSessionId, contentId);
    WatchingSessionDto returnedWatchingSession = returnedWatchingSessionChange.watchingSession();

    // then
    assertThat(returnedWatchingSessionChange.type()).isEqualTo(ChangeType.LEAVE);
    assertThat(returnedWatchingSession.id()).isEqualTo(watchingSessionId);
  }

  @Test
  @DisplayName("leaveSession - 세션 존재하지 않으면 실패")
  void leaveSessionNonExistentSession() {
    // given
    when(watchingSessionRepository.findById(any(UUID.class)))
        .thenThrow(new WatchingSessionNotFoundException(
            WatchingSessionErrorCode.WATCHING_SESSION_NOT_FOUND,
            Map.of("watchingSessionId", watchingSessionId))
        );

    // when & then
    assertThrows(WatchingSessionNotFoundException.class, () -> {
      watchingSessionService.leaveSession(userId, watchingSessionId, contentId);
    });
  }

  @Test
  @DisplayName("getWatcherCounts - 캐시 히트 성공")
  void getWatcherCounts_AllCacheHits() {
    // given
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    List<UUID> contentIds = List.of(id1, id2);

    List<String> cachedValues = Arrays.asList("100", "200");
    when(valueOperations.multiGet(anyList())).thenReturn(cachedValues);

    // when
    Map<UUID, Long> result = watchingSessionService.getWatcherCounts(contentIds);

    // then
    assertThat(result.get(id1)).isEqualTo(100L);
    assertThat(result.get(id2)).isEqualTo(200L);
    verify(watchingSessionRepository, never()).countByContentId(any());
    verify(valueOperations, never()).set(any(String.class), any(String.class));
  }

  @Test
  @DisplayName("getWatcherCounts - 캐시 미스 (DB 조회)")
  void getWatcherCounts_AllCacheMiss() {
    // given
    UUID id1 = UUID.randomUUID();
    List<UUID> contentIds = List.of(id1);
    List<String> cachedValues = Collections.singletonList(null);
    when(valueOperations.multiGet(anyList())).thenReturn(cachedValues);
    when(watchingSessionRepository.countByContentId(id1)).thenReturn(50L);

    // when
    Map<UUID, Long> result = watchingSessionService.getWatcherCounts(contentIds);

    // then
    assertThat(result.get(id1)).isEqualTo(50L);
    verify(watchingSessionRepository).countByContentId(id1);
    verify(valueOperations).set(argThat(key -> key.endsWith(id1.toString())), eq("50"));
  }
}
