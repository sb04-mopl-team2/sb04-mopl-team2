package com.codeit.mopl.domain.notification.repository;

import com.codeit.mopl.domain.base.TimeUtil;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements CustomNotificationRepository {

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
      where.and(qNotification.id.notIn(idAfter));
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

  private List<OrderSpecifier<?>> buildOrderSpecifiers(
      SortBy sortBy,
      SortDirection sortDirection,
      QNotification q
  ) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();

    if (sortBy != null && sortDirection != null) {
      Order order = (sortDirection == SortDirection.DESCENDING)
          ? Order.DESC
          : Order.ASC;

      switch (sortBy) {
        case CREATED_AT:
          orders.add(new OrderSpecifier<>(order, q.createdAt));
          orders.add(new OrderSpecifier<>(order, q.id));
          break;
      }
    }

    return orders;
  }

  private BooleanExpression buildCursorCondition(
      String cursor,
      UUID idAfter,
      SortBy sortBy,
      SortDirection sortDirection,
      QNotification q
  ) {
    if (cursor == null || sortBy == null || sortDirection == null) {
      return null;
    }

    // cursor: KST 기준 LocalDateTime 문자열이라고 가정
    LocalDateTime cursorLocalDateTime = LocalDateTime.parse(cursor);
    Instant cursorInstant = TimeUtil.toInstant(cursorLocalDateTime);

    // createdAt(Instant) + id 조합으로 커서 조건 생성
    if (sortDirection == SortDirection.DESCENDING) {
      return q.createdAt.lt(cursorInstant)
          .or(q.createdAt.eq(cursorInstant).and(q.id.lt(idAfter)));
    } else {
      return q.createdAt.gt(cursorInstant)
          .or(q.createdAt.eq(cursorInstant).and(q.id.gt(idAfter)));
    }
  }

}
