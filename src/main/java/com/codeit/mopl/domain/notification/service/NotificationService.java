package com.codeit.mopl.domain.notification.service;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  public CursorResponseNotificationDto getNotifications(String cursor, UUID idAfter, int limit, SortDirection sortDirection, SortBy sortBy) {
    return null;
  }

}
