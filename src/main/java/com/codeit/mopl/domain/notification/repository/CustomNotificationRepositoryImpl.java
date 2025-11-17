package com.codeit.mopl.domain.notification.repository;

import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.QNotification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Notification> searchNotifications(
      UUID userId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      SortBy sortBy){

    QNotification qNotification = QNotification.notification;

    BooleanBuilder where = new BooleanBuilder();
    where.and(qNotification.user.id.eq(userId));
    where.and(qNotification.status.eq(Status.UNREAD));


    if (cursor != null && idAfter != null) {
      where.and(buildCursorCondition(cursor, idAfter, sortBy, sortDirection, qNotification));
    }

    List<OrderSpecifier<?>> orders = buildOrderSpecifiers(sortBy, sortDirection, qNotification);

    List<Notification> notifications = queryFactory
        .selectFrom(qNotification)
        .where(where)
        .orderBy(orders.toArray(OrderSpecifier[]::new))
        .limit(limit+1)
        .fetch();

    return notifications;
  }

  private List<OrderSpecifier<?>> buildOrderSpecifiers(SortBy sortBy, SortDirection sortDirection, QNotification qnotification) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();

    if (sortBy != null && sortDirection != null) {
      Order order = sortDirection.equals(SortDirection.DESCENDING) ? Order.DESC : Order.ASC;
      switch (sortBy.toString()) {
        case "createdAt":
          orders.add(new OrderSpecifier<>(order, qnotification.createdAt));
          break;
      }
    }

    orders.add(new OrderSpecifier<>(Order.DESC, qnotification.createdAt));
    return orders;
  }

  private BooleanExpression buildCursorCondition(String cursor, UUID idAfter, SortBy sortBy, SortDirection sortDirection, QNotification qnotification) {

    if (sortBy == null || sortDirection == null) {
      return null;
    }

    LocalDateTime cursorTime = cursor != null ? LocalDateTime.parse(cursor) : null;

    BooleanExpression condition  = null;

    switch (sortBy.toString()) {
      case "createdAt": {
        if (sortDirection == SortDirection.DESCENDING) {
          condition = qnotification.createdAt.lt(cursorTime);
        }
        else {
          condition = qnotification.createdAt.gt(cursorTime);
        }
        break;
      }

      default:
        break;
    }
    return condition;
  }
}
