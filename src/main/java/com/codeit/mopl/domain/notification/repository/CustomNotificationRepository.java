package com.codeit.mopl.domain.notification.repository;

import com.codeit.mopl.domain.base.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.notification.entity.Notification;
import java.util.List;
import java.util.UUID;

public interface CustomNotificationRepository {

  List<Notification> searchNotifications(
      UUID userId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      SortBy sortBy);
}
