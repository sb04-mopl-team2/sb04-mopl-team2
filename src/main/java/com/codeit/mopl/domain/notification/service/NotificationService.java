package com.codeit.mopl.domain.notification.service;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  public CursorResponseNotificationDto getNotifications(UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy) {

    List<NotificationDto> data = null;
    String nextCursor = null; // 시간 형태임
    UUID nextIdAfter = null; // 가장 마지막 알림의 id
    Boolean hasNext = false; // 다음 페이지가 있는지
    Long totalCount = 0L; // 총 데이터의 개수

    List<Notification> notificationList;
    if (cursor == null) {
      notificationList = getNotificationWithoutCursor(userId, limit+1, sortDirection, sortBy);
    }
    else {
      notificationList = getNotificationWithCursor(userId, cursor, idAfter, limit+1, sortDirection, sortBy);
    }
    // 경우의 수가 2가지 밖에 없어서 QueryDsl을 안사용해도 무방함, 나중에 QueryDsl을 이용해서 고도화도 고려하기

    if (notificationList.isEmpty()) {
      return new CursorResponseNotificationDto(null, null, null, false, 0L, SortBy.createdAt, SortDirection.DESCENDING);
    }

    if (notificationList.size() > limit) {
      nextCursor = notificationList.get(limit-1).getCreatedAt().toString();
      hasNext = true;
      nextIdAfter = notificationList.get(limit-1).getId(); // 보여지는 알림 중  가장 마지막 알림의 id
    }

    data = notificationList.stream()
        .map(notificationMapper::toDto)
        .toList();


    totalCount = getTotalCount(userId);
    return new CursorResponseNotificationDto(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
  }

  private List<Notification> getNotificationWithCursor(UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy){
    return null;
  }

  private List<Notification> getNotificationWithoutCursor(UUID userId, int limit, SortDirection sortDirection, SortBy sortBy){
    return null;
  }

  private Long getTotalCount(UUID userId){
    return notificationRepository.countByReceiverIdAndStatus(userId,
        Status.UNREAD);
  }

}
