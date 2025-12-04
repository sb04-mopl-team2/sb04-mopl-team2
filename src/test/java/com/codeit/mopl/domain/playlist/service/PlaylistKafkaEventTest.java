package com.codeit.mopl.domain.playlist.service;

import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.subscription.repository.SubscriptionRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.PlayListCreateEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PlaylistService.createPlaylist() 의 이벤트 발행 검증
 */
@ExtendWith(MockitoExtension.class)
class PlaylistKafkaEventTest {

  @Mock
  private PlaylistRepository playlistRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PlaylistMapper playlistMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  private PlaylistService playlistService;

  @BeforeEach
  void setUp() {
    playlistService = new PlaylistService(
        userRepository,
        playlistRepository,
        playlistMapper,
        eventPublisher,
        subscriptionRepository
    );
  }

  @Test
  @DisplayName("createPlaylist 호출 시 PlayListCreateEvent 를 발행한다")
  void createPlaylist_publishesPlayListCreateEvent() {
    // given
    UUID ownerId = UUID.randomUUID();
    PlaylistCreateRequest request =
        new PlaylistCreateRequest("테스트 플레이리스트", "설명입니다");

    // 유저 조회 성공
    User user = mock(User.class);
    when(userRepository.findById(ownerId))
        .thenReturn(Optional.of(user));

    // 저장된 Playlist 모킹 (id, title 리턴)
    UUID playlistId = UUID.randomUUID();
    Playlist savedPlaylist = mock(Playlist.class);
    when(savedPlaylist.getId()).thenReturn(playlistId);
    when(savedPlaylist.getTitle()).thenReturn(request.title());

    when(playlistRepository.save(any(Playlist.class)))
        .thenReturn(savedPlaylist);

    // mapper 는 단순히 호출만 되면 되므로 mock 반환
    PlaylistDto playlistDto = mock(PlaylistDto.class);
    when(playlistMapper.toPlaylistDto(savedPlaylist))
        .thenReturn(playlistDto);

    // when
    PlaylistDto result = playlistService.createPlaylist(ownerId, request);

    // then
    // 1) 반환값은 mapper 가 돌려준 dto
    assertThat(result).isSameAs(playlistDto);

    // 2) 이벤트 발행 여부 및 내용 검증
    ArgumentCaptor<PlayListCreateEvent> eventCaptor =
        ArgumentCaptor.forClass(PlayListCreateEvent.class);

    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    PlayListCreateEvent publishedEvent = eventCaptor.getValue();
    assertThat(publishedEvent.playListId()).isEqualTo(playlistId);
    assertThat(publishedEvent.ownerId()).isEqualTo(ownerId);
    assertThat(publishedEvent.title()).isEqualTo(request.title());

    // 3) save 호출도 한 번 했는지 (선택)
    verify(playlistRepository, times(1)).save(any(Playlist.class));
  }
}
