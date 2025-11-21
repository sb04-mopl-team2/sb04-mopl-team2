package com.codeit.mopl.domain.review.repository;

import com.codeit.mopl.domain.review.entity.QReview;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
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
public class ReviewRepositoryImpl implements CustomReviewRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Review> searchReview(
      UUID contentId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      ReviewSortBy sortBy){

    QReview qReview = QReview.review;

    BooleanBuilder where = new BooleanBuilder();
    where.and(qReview.content.id.eq(contentId));
    where.and(qReview.isDeleted.eq(false));

    if (cursor != null && idAfter != null) {
      where.and(buildCursorCondition(cursor, idAfter, sortBy, sortDirection, qReview));
    }

    List<OrderSpecifier<?>> orders = buildOrderSpecifiers(sortBy, sortDirection, qReview);

    List<Review> reviewList = queryFactory
        .selectFrom(qReview)
        .where(where)
        .orderBy(orders.toArray(OrderSpecifier[]::new))
        .limit(limit+1)
        .fetch();

    return reviewList;
  }

  private List<OrderSpecifier<?>> buildOrderSpecifiers(ReviewSortBy sortBy, SortDirection sortDirection, QReview qReview) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();

    if (sortBy != null && sortDirection != null) {
      Order order = sortDirection.equals(SortDirection.DESCENDING) ? Order.DESC : Order.ASC;
      switch (sortBy) {
        case createdAt:
          orders.add(new OrderSpecifier<>(order, qReview.createdAt));
          break;
        case rating:
          orders.add(new OrderSpecifier<>(order, qReview.rating));
          break;
      }
    }

    return orders;
  }

  private BooleanExpression buildCursorCondition(String cursor, UUID idAfter, ReviewSortBy sortBy, SortDirection sortDirection, QReview qReview) {

    if (sortBy == null || sortDirection == null) {
      return null;
    }

    LocalDateTime cursorTime = cursor != null ? LocalDateTime.parse(cursor) : null;

    BooleanExpression condition  = null;

    switch (sortBy.toString()) {
      case "createdAt": {
        if (sortDirection == SortDirection.DESCENDING) {
          condition = qReview.createdAt.lt(cursorTime);
        }
        else {
          condition = qReview.createdAt.gt(cursorTime);
        }
        break;
      }

      default:
        break;
    }
    return condition;
  }
}
