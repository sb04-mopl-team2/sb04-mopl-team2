package com.codeit.mopl.domain.notification.repository;

import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

public interface CustomNotificationRepository {

  List<Notification> searchNotifications(
      UUID userId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      SortBy sortBy);
}
