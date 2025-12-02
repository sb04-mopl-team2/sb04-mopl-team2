package com.codeit.mopl.domain.watchingsession.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.codeit.mopl.domain.watchingsession.mapper.WatchingSessionMapper;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.mopl.exception.content.ContentNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class WatchingSessionServiceTest {

  @Mock
  private WatchingSessionRepository watchingSessionRepository;

  @Mock
  private WatchingSessionMapper watchingSessionMapper;

  @Mock
  private ContentRepository contentRepository;


  @InjectMocks
  private WatchingSessionService watchingSessionService;

  private User user;
  private Content content;
  private WatchingSession entity;

  @BeforeEach
  void init() {
    user = new User("test@test.com", "pw", "test");
    content = new Content();
    content.setTitle("testTitleContent");

    entity = new WatchingSession();
    entity.setUser(user);
    entity.setContent(content);
  }

  /*
    public WatchingSessionDto getByUserId(UUID userId)
   */
  @Test
  @DisplayName("UserId로 조회 성공")
  void getByUserIdSuccess() {
    // given
    UUID userId = UUID.randomUUID();
    when(watchingSessionRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
    WatchingSessionDto expectedDto = mock(WatchingSessionDto.class);
    when(watchingSessionMapper.toDto(entity)).thenReturn(expectedDto);

    // when
    WatchingSessionDto result = watchingSessionService.getByUserId(userId);

    // then
    assertEquals(expectedDto, result);
    verify(watchingSessionMapper).toDto(entity);
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
        eq(11), // internal limit
        any(), any()
    )).thenReturn(List.of(entity));
    CursorResponseWatchingSessionDto expectedDto = new CursorResponseWatchingSessionDto(
        List.of(watchingSessionDto),
        null, null, false,
        1L, SortBy.CREATED_AT.getType(), SortDirection.ASCENDING
    );
    when(contentRepository.existsById(contentId)).thenReturn(true);
    when(watchingSessionMapper.toDto(entity)).thenReturn(watchingSessionDto);
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
    // entity2 setup
    WatchingSession entity2 = new WatchingSession();
    entity2.setUser(user2);
    entity2.setContent(content);
    UUID entity2Id = UUID.randomUUID();
    LocalDateTime entity2Time = LocalDateTime.now();
    ReflectionTestUtils.setField(entity2, "id", entity2Id);
    ReflectionTestUtils.setField(entity2, "createdAt", entity2Time);

    List<WatchingSession> mutableList = new ArrayList<>();
    mutableList.add(entity);
    mutableList.add(entity2);

    when(watchingSessionRepository.findWatchingSessions(
        any(), any(), any(), any(),
        eq(2), // internal limit (1 + 1)
        any(), any()
    )).thenReturn(mutableList);

    // mapper, count
    WatchingSessionDto watchingSessionDto = mock(WatchingSessionDto.class);
    when(contentRepository.existsById(contentId)).thenReturn(true);
    when(watchingSessionMapper.toDto(entity)).thenReturn(watchingSessionDto);
    when(watchingSessionRepository.getWatcherCount(contentId, null)).thenReturn(2L);

    // expected response
    CursorResponseWatchingSessionDto expectedDto = new CursorResponseWatchingSessionDto(
        List.of(watchingSessionDto),
        entity2Time.toString(),
        entity2Id,
        true,
        2L, SortBy.CREATED_AT.getType(), SortDirection.ASCENDING
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
}
