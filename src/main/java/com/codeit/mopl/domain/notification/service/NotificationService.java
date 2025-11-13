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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final RepositoryNotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  @Transactional(readOnly = true)
  public CursorResponseNotificationDto getNotifications(UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy) {

    List<NotificationDto> data = null;
    String nextCursor = null;
    UUID nextIdAfter = null;
    Boolean hasNext = false;
    Long totalCount = 0L;

    List<Notification> notificationList;
    notificationList = searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);

    if (notificationList.isEmpty()) {
      return new CursorResponseNotificationDto(null, null, null, false, 0L, SortBy.createdAt, SortDirection.DESCENDING);
    }

    if (notificationList.size() > limit) {
      notificationList = notificationList.subList(0, limit);
      nextCursor = notificationList.get(limit-1).getCreatedAt().toString();
      nextIdAfter = notificationList.get(limit-1).getId();
      hasNext = true;
    }

    data = notificationList.stream()
        .map(notificationMapper::toDto)
        .toList();


    totalCount = getTotalCount(userId);
    return new CursorResponseNotificationDto(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
  }

  public void deleteNotification(UUID userId ,UUID notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(NotificationNotFoundException::new);

    if (!notification.getUser().getId().equals(userId)){
      throw new NotificationNotAuthentication();
    }
    notification.setStatus(Status.READ);
    notificationRepository.save(notification);
  }

  private List<Notification> searchNotifications(UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy){
    return notificationRepository.searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);
  }

  private Long getTotalCount(UUID userId){
    return notificationRepository.countByUserIdAndStatus(userId,
        Status.UNREAD);
  }
}
