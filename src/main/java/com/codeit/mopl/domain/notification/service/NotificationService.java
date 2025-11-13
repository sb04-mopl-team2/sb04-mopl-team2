package com.codeit.mopl.domain.notification.service;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.RepositoryNotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final RepositoryNotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  public CursorResponseNotificationDto getNotifications(UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy) {

    List<NotificationDto> data = null;
    String nextCursor = null; // 시간 형태임
    UUID nextIdAfter = null; // 가장 마지막 알림의 id
    Boolean hasNext = false; // 다음 페이지가 있는지
    Long totalCount = 0L; // 총 데이터의 개수

    List<Notification> notificationList;
    notificationList = searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);
    // 경우의 수가 2가지 밖에 없어서 QueryDsl을 안사용해도 무방함, 나중에 QueryDsl을 이용해서 고도화도 고려하기

    if (notificationList.isEmpty()) {
      return new CursorResponseNotificationDto(null, null, null, false, 0L, SortBy.createdAt, SortDirection.DESCENDING);
    }

    if (notificationList.size() > limit) {
      notificationList = notificationList.subList(0, limit); // limit+1 번째의 값을 제거함, 가장 첫 번 째의 값은 get(0) 이므로 limit+1의 값은 get(limit)임
      nextCursor = notificationList.get(limit-1).getCreatedAt().toString();
      nextIdAfter = notificationList.get(limit-1).getId(); // 가장 마지막 알림의 id
      hasNext = true;
    }

    data = notificationList.stream()
        .map(notificationMapper::toDto)
        .toList();


    totalCount = getTotalCount(userId);
    return new CursorResponseNotificationDto(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
  }

  private List<Notification> searchNotifications(UUID userId, String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy){
    return notificationRepository.searchNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);
  }

  private Long getTotalCount(UUID userId){
    return notificationRepository.countByUserIdAndStatus(userId,
        Status.UNREAD);
  }
}
