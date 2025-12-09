package com.codeit.mopl.domain.notification.service;

import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.PlayListCreateEvent;
import com.codeit.mopl.event.event.WatchingSessionCreateEvent;
import com.codeit.mopl.sse.service.SseService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowCreateNotificationTest {

  @Mock
  private FollowRepository followRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private NotificationMapper notificationMapper;

  @Spy
  @InjectMocks
  private NotificationService notificationService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private SseService sseService;

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @Test
  @DisplayName("notifyFollowersOnPlaylistCreated - 팔로워 각각에게 알림이 발송된다")
  void notifyFollowersOnPlaylistCreated_sendsNotificationsToFollowers() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    String playlistTitle = "내 플레이리스트";

    PlayListCreateEvent event =
        new PlayListCreateEvent(playlistId, ownerId, playlistTitle);

    // 팔로워/팔로이 유저 모킹
    User followee = mock(User.class);
    when(followee.getName()).thenReturn("관리자 유저");

    User follower1 = mock(User.class);
    User follower2 = mock(User.class);
    UUID follower1Id = UUID.randomUUID();
    UUID follower2Id = UUID.randomUUID();
    when(follower1.getId()).thenReturn(follower1Id);
    when(follower2.getId()).thenReturn(follower2Id);

    Follow follow1 = mock(Follow.class);
    Follow follow2 = mock(Follow.class);

    when(follow1.getFollower()).thenReturn(follower1);
    when(follow1.getFollowee()).thenReturn(followee);

    when(follow2.getFollower()).thenReturn(follower2);
    when(follow2.getFollowee()).thenReturn(followee);

    when(followRepository.findByFolloweeId(ownerId))
        .thenReturn(List.of(follow1, follow2));

    // 현재 구현 로그 기준으로 기대값 맞추기
    String expectedTitle = "새 플레이리스트가 추가됐어요";
    String expectedContent =
        "관리자 유저님이 새로운 플레이리스트 " + playlistTitle + "를 만들었어요!";

    // createNotification 내부에서 userRepository.findById(...) 사용하므로 스텁
    when(userRepository.findById(follower1Id))
        .thenReturn(Optional.of(follower1));
    when(userRepository.findById(follower2Id))
        .thenReturn(Optional.of(follower2));

    // when
    notificationService.notifyFollowersOnPlaylistCreated(event);

    // then
    verify(followRepository, times(1)).findByFolloweeId(ownerId);

    // 각 팔로워에게 알림이 한 번씩 발행되는지 검증
    verify(notificationService, times(1))
        .createNotification(
            eq(follower1Id),
            eq(expectedTitle),
            eq(expectedContent),
            eq(Level.INFO)
        );

    verify(notificationService, times(1))
        .createNotification(
            eq(follower2Id),
            eq(expectedTitle),
            eq(expectedContent),
            eq(Level.INFO)
        );
  }

  @Test
  @DisplayName("notifyFollowersOnWatchingEvent - 팔로워 각각에게 알림이 발송된다")
  void notifyFollowersOnWatchingEvent_sendsNotificationsToFollowers() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID watchingSessionId = UUID.randomUUID();
    String contentTitle = "실시간 콘텐츠 제목";

    WatchingSessionCreateEvent event =
        new WatchingSessionCreateEvent(watchingSessionId, ownerId, contentTitle);

    User followee = mock(User.class);
    when(followee.getName()).thenReturn("관리자 유저");

    User follower1 = mock(User.class);
    User follower2 = mock(User.class);
    UUID follower1Id = UUID.randomUUID();
    UUID follower2Id = UUID.randomUUID();
    when(follower1.getId()).thenReturn(follower1Id);
    when(follower2.getId()).thenReturn(follower2Id);

    Follow follow1 = mock(Follow.class);
    Follow follow2 = mock(Follow.class);

    when(follow1.getFollower()).thenReturn(follower1);
    when(follow1.getFollowee()).thenReturn(followee);

    when(follow2.getFollower()).thenReturn(follower2);
    when(follow2.getFollowee()).thenReturn(followee);

    when(followRepository.findByFolloweeId(ownerId))
        .thenReturn(List.of(follow1, follow2));

    // createNotification 내부에서 userRepository.findById(...) 사용하므로 스텁
    when(userRepository.findById(follower1Id))
        .thenReturn(Optional.of(follower1));
    when(userRepository.findById(follower2Id))
        .thenReturn(Optional.of(follower2));

    // when
    notificationService.notifyFollowersOnWatchingEvent(event);

    // then
    verify(followRepository, times(1)).findByFolloweeId(ownerId);

    // 시청 알림은 메시지 포맷이 바뀔 수 있으니, content 안에 contentTitle 이 포함되는지만 확인
    verify(notificationService, times(1))
        .createNotification(
            eq(follower1Id),
            anyString(),
            contains(contentTitle),
            eq(Level.INFO)
        );

    verify(notificationService, times(1))
        .createNotification(
            eq(follower2Id),
            anyString(),
            contains(contentTitle),
            eq(Level.INFO)
        );
  }
}
