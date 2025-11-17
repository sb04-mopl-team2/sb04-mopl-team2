package com.codeit.mopl.domain.notification.service;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.domain.notification.exception.NotificationNotAuthentication;
import com.codeit.mopl.domain.notification.exception.NotificationNotFoundException;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.RepositoryNotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final RepositoryNotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

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

    if (notificationList.isEmpty()) {
      log.debug("[알림] 알림 리스트가 비었음, userId = {}", userId);
      CursorResponseNotificationDto cursorResponseNotificationDto = new CursorResponseNotificationDto(
          null, null, null, false, 0L, SortBy.createdAt, SortDirection.DESCENDING);

      log.info("[알림] 알림 조회 종료, userId={}, resultSize={}, hasNext={}, totalCount={}",
          userId, cursorResponseNotificationDto.data().size(), cursorResponseNotificationDto.hasNext(), cursorResponseNotificationDto.totalCount());
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

    log.info("[알림] 알림 조회 종료, userId={}, resultSize={}, hasNext={}, totalCount={}",
        userId, data.size(), hasNext, totalCount);

    CursorResponseNotificationDto cursorResponseNotificationDto = new CursorResponseNotificationDto(
        data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);

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
      throw new NotificationNotAuthentication();
    }

    notification.setStatus(Status.READ);
    notificationRepository.save(notification);

    log.info("[알림] 알림 삭제 종료, notificationId={}", notificationId);
  }

  private List<Notification> searchNotifications(
      UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy) {
    return notificationRepository.searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);
  }

  private Long getTotalCount(UUID userId) {
    return notificationRepository.countByUserIdAndStatus(userId, Status.UNREAD);
  }
}
