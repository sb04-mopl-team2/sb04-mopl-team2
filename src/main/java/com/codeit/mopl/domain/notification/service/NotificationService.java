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
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
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

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  @Transactional(readOnly = true)
  public CursorResponseNotificationDto getNotifications(
      UUID userId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      SortBy sortBy) {

    log.info("[알림 리스트 요청] userId={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
        userId, cursor, idAfter, limit, sortBy, sortDirection);

    List<Notification> notificationList =
        searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);

    log.debug("[알림 리스트 결과] 개수={}", notificationList.size());

    if (notificationList.isEmpty()) {
      log.info("[알림 리스트가 비었음 userId={}", userId);
      return new CursorResponseNotificationDto(
          null, null, null, false, 0L, SortBy.createdAt, SortDirection.DESCENDING
      );
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = false;

    if (notificationList.size() > limit) {
      log.debug("[알림 리스트가 Limt 값 보다 큼]");
      notificationList = notificationList.subList(0, limit);
      nextCursor = notificationList.get(limit - 1).getCreatedAt().toString();
      nextIdAfter = notificationList.get(limit - 1).getId();
      hasNext = true;
    }

    List<NotificationDto> data = notificationList.stream()
        .map(notificationMapper::toDto)
        .toList();

    long totalCount = getTotalCount(userId);

    log.info("[커서페이지네이션 알림 리스트 결과] userId={}, resultSize={}, hasNext={}, totalCount={}",
        userId, data.size(), hasNext, totalCount);

    return new CursorResponseNotificationDto(
        data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
  }

  @Transactional
  public void deleteNotification(UUID userId, UUID notificationId) {

    log.info("[알림 삭제 요청] userId={}, notificationId={}", userId, notificationId);

    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> {
          log.warn("[알림 삭제 실패] Not found notificationId={}", notificationId);
          return new NotificationNotFoundException();
        });

    UUID ownerId = notification.getUser().getId();
    if (!ownerId.equals(userId)) {
      log.warn("[알림 삭제 실패] userId={} is not owner of notificationId={}, ownerId={}",
          userId, notificationId, ownerId);
      throw new NotificationNotAuthentication();
    }


    notification.setStatus(Status.READ);

    notificationRepository.save(notification);
    log.info("[알림 삭제 성공] notificationId={}", notificationId);
  }

  private List<Notification> searchNotifications(
      UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy) {

    log.debug("[알림 조회] userId={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
        userId, cursor, idAfter, limit, sortBy, sortDirection);

    return notificationRepository.searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);
  }

  private Long getTotalCount(UUID userId) {
    long count = notificationRepository.countByUserIdAndStatus(userId, Status.UNREAD);
    log.debug("[알림 총 개수 조회] userId={}, unreadCount={}", userId, count);
    return count;
  }
}
