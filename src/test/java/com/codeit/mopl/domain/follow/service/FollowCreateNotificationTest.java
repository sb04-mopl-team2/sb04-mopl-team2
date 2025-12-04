package com.codeit.mopl.domain.follow.service;

import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.mapper.FollowMapper;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.PlayListCreateEvent;
import com.codeit.mopl.event.event.WatchingSessionCreateEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowCreateNotificationTest {

  @Mock
  private FollowRepository followRepository;

  @Mock
  private FollowMapper followMapper;

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private ProcessedEventRepository processedEventRepository;

  @InjectMocks
  private FollowService followService;

  @Test
  @DisplayName("notifyFollowersOnPlaylistCreated - 팔로워 각각에게 알림이 발송된다")
  void notifyFollowersOnPlaylistCreated_sendsNotificationsToFollowers() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    String playlistTitle = "내 플레이리스트";

    // 이벤트 (record 라고 가정)
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

    String expectedTitle =
        "관리자 유저님이 새로운 플레이리스트: " + playlistTitle + "를 만들었어요!";

    // when
    followService.notifyFollowersOnPlaylistCreated(event);

    // then
    verify(followRepository, times(1)).findByFolloweeId(ownerId);
    verify(notificationService, times(1))
        .createNotification(follower1Id, expectedTitle, "", Level.INFO);
    verify(notificationService, times(1))
        .createNotification(follower2Id, expectedTitle, "", Level.INFO);
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

    String expectedTitle =
        "관리자 유저님이 " + contentTitle + "를 보고있어요!";

    // when
    followService.notifyFollowersOnWatchingEvent(event);

    // then
    verify(followRepository, times(1)).findByFolloweeId(ownerId);
    verify(notificationService, times(1))
        .createNotification(follower1Id, expectedTitle, "", Level.INFO);
    verify(notificationService, times(1))
        .createNotification(follower2Id, expectedTitle, "", Level.INFO);
  }
}
