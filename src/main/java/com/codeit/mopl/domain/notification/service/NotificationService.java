package com.codeit.mopl.domain.notification.service;

import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.event.event.PlayListCreateEvent;
import com.codeit.mopl.event.event.WatchingSessionCreateEvent;
import com.codeit.mopl.exception.notification.NotificationForbidden;
import com.codeit.mopl.exception.notification.NotificationNotFoundException;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.sse.service.SseService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final SseService sseService;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final StringRedisTemplate stringRedisTemplate;
  private final FollowRepository followRepository;

  public static final String NOTIFICATIONS_FIRST_PAGE = "notifications:first-page";

  @Cacheable(
      cacheNames = NOTIFICATIONS_FIRST_PAGE,
      key = "T(java.lang.String).format('%s:%s:%s:%s', #userId, #limit, #sortDirection, #sortBy)",
      condition = "#cursor == null && #idAfter == null"
  )
  @Transactional(readOnly = true)
  public CursorResponseNotificationDto getNotifications(
      UUID userId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      SortBy sortBy) {

    log.info("[알림] 알림 조회 시작, userId = {}, cursor = {}, idAfter = {}, limit = {}, sortBy = {}, sortDirection = {}",
        userId, cursor, idAfter, limit, sortBy, sortDirection);

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = false;

    List<Notification> notificationList =
        searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);

    String responseSortBy = sortBy == SortBy.CREATED_AT ? "createdAt" : "updatedAt";

    if (notificationList.isEmpty()) {
      log.debug("[알림] 알림 리스트가 비었음, userId = {}", userId);
      CursorResponseNotificationDto cursorResponseNotificationDto = new CursorResponseNotificationDto(
          List.of(), null, null, false, 0L, responseSortBy, SortDirection.DESCENDING);

      log.info("[알림] 알림 조회 종료, userId = {}, notificationListSize = {}, hasNext = {}, totalCount = {}",
          userId, 0L, cursorResponseNotificationDto.hasNext(), cursorResponseNotificationDto.totalCount());
      return cursorResponseNotificationDto;
    }

    if (notificationList.size() > limit) {
      log.debug("[알림] 알림 리스트의 사이즈가 limit 값 보다 큼, limit = {}, ListSize = {}", limit, notificationList.size());
      notificationList = notificationList.subList(0, limit);
      nextCursor = notificationList.get(limit - 1).getCreatedAt().toString();
      nextIdAfter = notificationList.get(limit - 1).getId();
      hasNext = true;
    }

    List<NotificationDto> data = notificationList.stream()
        .map(notificationMapper::toDto)
        .toList();

    long totalCount = getTotalCount(userId);

    log.info("[알림] 알림 조회 종료, userId = {}, notificationListSize = {}, hasNext = {}, totalCount = {}",
        userId, data.size(), hasNext, totalCount);

    CursorResponseNotificationDto cursorResponseNotificationDto = new CursorResponseNotificationDto(
        data, nextCursor, nextIdAfter, hasNext, totalCount, responseSortBy, sortDirection);

    log.info("[알림] 알림 조회 종료, userId={}, resultSize={}, hasNext={}, totalCount={}",
        userId, cursorResponseNotificationDto.data().size(), cursorResponseNotificationDto.hasNext(), cursorResponseNotificationDto.totalCount());

    return cursorResponseNotificationDto;
  }

  @Transactional
  public void deleteNotification(UUID userId, UUID notificationId) {

    log.info("[알림] 알림 삭제 시작, userId = {}, notificationId = {}", userId, notificationId);

    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> {
          log.warn("[알림] 알림 삭제 실패, 알림을 찾을 수 없음, notificationId = {}", notificationId);
          return new NotificationNotFoundException();
        });

    UUID ownerId = notification.getUser().getId();
    if (!ownerId.equals(userId)) {
      log.warn("[알림] 알림 삭제 실패, 알림을 삭제할 권한이 없음, userId = {}, notificationId = {}, ownerId = {}",
          userId, notificationId, ownerId);
      throw new NotificationForbidden();
    }

    notification.setStatus(Status.READ);
    notificationRepository.save(notification);

    evictFirstPageCacheByUserId(userId);
    log.info("[알림] 알림 삭제 종료, notificationId={}", notificationId);
  }

  @Transactional
  public void createNotification(UUID userId, String title, String content, Level level) {
    log.info("[알림] 알림 생성 시작, userId = {}, title = {}, content = {}, level = {}", userId, title, content, level);

    Notification notification = saveNotification(userId, title, content, level);
    NotificationDto notificationDto = notificationMapper.toDto(notification);
    eventPublisher.publishEvent(new NotificationCreateEvent(notificationDto));

    log.info("[알림] 알림 생성 종료, userId = {}, notificationId = {}", userId, notification.getId());
  }

  public void sendNotification(NotificationDto notificationDto) {
    log.info("[알림] 알림 생성 SSE 전송 호출 시작, notificationDto = {}", notificationDto);

    UUID receiverId = notificationDto.receiverId();
    String eventName = "notifications";
    Object data = notificationDto;
    sseService.send(receiverId, eventName, data);
    log.info("[알림] 알림 생성 SSE 전송 호출 종료, notificationDto = {}", notificationDto);
  }

  @Transactional
  public void sendDirectMessage(DirectMessageDto directMessageDto) {
    log.info("[알림] DM 생성 SSE 전송 호출 시작, notificationDto = {}", directMessageDto);

    UUID receiverId = directMessageDto.receiver().userId();
    String eventName = "direct-messages";
    Object data = directMessageDto;

    String title = "[DM] " + directMessageDto.receiver().name();
    String content = directMessageDto.content();
    Level level = Level.INFO;
    createNotification(receiverId, title, content, level);
    sseService.send(receiverId, eventName, data);
    log.info("[알림] DM 생성 SSE 전송 호출 종료, notificationDto = {}", directMessageDto);
  }

  @Transactional
  public void notifyFollowersOnPlaylistCreated(PlayListCreateEvent playListCreateEvent) {
    log.info("[팔로우 관리] 팔로우한 유저가 플레이리스트 생성시 팔로워 알림 송신 시작 : playListId = {}", playListCreateEvent.playListId());
    UUID ownerId = playListCreateEvent.ownerId();
    List<Follow> followList = followRepository.findByFolloweeId(ownerId);

    for (Follow follow : followList) {
      UUID receiverId = follow.getFollower().getId();
      String title = follow.getFollowee().getName() + "님이 새로운 플레이리스트: " + playListCreateEvent.title() + "를 만들었어요!";
      createNotification(receiverId, title, "", Level.INFO);
    }
    log.info("[팔로우 관리] 팔로우한 유저가 플레이리스트 생성시 팔로워 알림 송신 완료 : playListId = {}", playListCreateEvent.playListId());
  }

  @Transactional
  public void notifyFollowersOnWatchingEvent(WatchingSessionCreateEvent watchingSessionCreateEvent) {
    log.info("[팔로우 관리] 팔로우한 유저가 실시간 콘텐츠 시청시 팔로워 알림 송신 시작 : watchingSessionId = {}", watchingSessionCreateEvent.watchingSessionId());
    UUID ownerId = watchingSessionCreateEvent.ownerId();
    List<Follow> followList = followRepository.findByFolloweeId(ownerId);

    for (Follow follow : followList) {
      UUID receiverId = follow.getFollower().getId();
      String title = follow.getFollowee().getName() + "님이 " + watchingSessionCreateEvent.watchingSessionContentTitle() + "를 보고있어요!";
      createNotification(receiverId, title, "", Level.INFO);
    }
    log.info("[팔로우 관리] 팔로우한 유저가 실시간 콘텐츠 시청시 알림 송신 완료 : watchingSessionId = {}", watchingSessionCreateEvent.watchingSessionId());
  }

  private Notification saveNotification(UUID userId, String title, String content, Level level) {
    User user = userRepository.findById(userId)
        .orElseThrow(() ->
            new UserNotFoundException(
                UserErrorCode.USER_NOT_FOUND,
                Map.of("userId", userId)
            )
        );

    Notification notification = new Notification();
    notification.setUser(user);
    notification.setTitle(title);
    notification.setContent(content);
    notification.setLevel(level);
    notificationRepository.save(notification);

    evictFirstPageCacheByUserId(userId);

    return notification;
  }

  private List<Notification> searchNotifications(
      UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy) {
    return notificationRepository.searchNotifications(userId, cursor, idAfter, limit, sortDirection,
        sortBy);
  }

  private Long getTotalCount(UUID userId) {
    return notificationRepository.countByUserIdAndStatus(userId, Status.UNREAD);
  }

  private void evictFirstPageCacheByUserId(UUID userId) {
    String pattern = NOTIFICATIONS_FIRST_PAGE + "::" + userId + ":*";

    Set<String> keys = stringRedisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      stringRedisTemplate.delete(keys);
      log.info("[알림] 알림 조회 캐싱 초기화, userId = {}, evictedKeys = {}", userId, keys.size());
    }
  }
}
