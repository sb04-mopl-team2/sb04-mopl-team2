package com.codeit.mopl.domain.notification.service;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.NotificationSortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.domain.notification.exception.NotificationForbidden;
import com.codeit.mopl.domain.notification.exception.NotificationNotFoundException;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.sse.service.SseService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

  @Transactional(readOnly = true)
  public CursorResponseNotificationDto getNotifications(
      UUID userId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      NotificationSortBy notificationSortBy) {

    log.info("[알림] 알림 조회 시작, userId = {}, cursor = {}, idAfter = {}, limit = {}, sortBy = {}, sortDirection = {}",
        userId, cursor, idAfter, limit, notificationSortBy, sortDirection);

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = false;

    List<Notification> notificationList =
        searchNotifications(userId, cursor, idAfter, limit, sortDirection, notificationSortBy);

    if (notificationList.isEmpty()) {
      log.debug("[알림] 알림 리스트가 비었음, userId = {}", userId);
      CursorResponseNotificationDto cursorResponseNotificationDto = new CursorResponseNotificationDto(
          null, null, null, false, 0L, NotificationSortBy.CREATED_AT, SortDirection.DESCENDING);

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
        data, nextCursor, nextIdAfter, hasNext, totalCount, notificationSortBy, sortDirection);

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

    log.info("[알림] 알림 삭제 종료, notificationId={}", notificationId);
  }

  @Transactional
  public void createNotification(UUID userId, String title, String content, Level level) {
    log.info("[알림] 알림 생성 시작, userId = {}, title = {}, content = {}, level = {}", userId, title, content, level);
    User user = userRepository.findById(userId).orElseThrow(); // UserNotFoundException 추후에 추가하기

    Notification notification = new Notification();
    notification.setUser(user);
    notification.setTitle(title);
    notification.setContent(content);
    notification.setLevel(level);
    notificationRepository.save(notification);

    NotificationDto notificationDto = notificationMapper.toDto(notification);
    eventPublisher.publishEvent(new NotificationCreateEvent(notificationDto));

    log.info("[알림] 알림 생성 종료, userId = {}, notificationId = {}", userId, notification.getId());
  }

  public void sendNotification(NotificationDto notificationDto) {
    log.info("[알림] SSE 전송 호출 시작, notificationDto = {}", notificationDto);

    UUID receiverId = notificationDto.receiverId();
    String eventName = "notification";
    Object data = notificationDto;
    sseService.send(receiverId, eventName, data);
    log.info("[알림] SSE 전송 호출 종료, notificationDto = {}", notificationDto);
  }

  private List<Notification> searchNotifications(
      UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, NotificationSortBy notificationSortBy) {
    return notificationRepository.searchNotifications(userId, cursor, idAfter, limit, sortDirection,
        notificationSortBy);
  }

  private Long getTotalCount(UUID userId) {
    return notificationRepository.countByUserIdAndStatus(userId, Status.UNREAD);
  }
}
