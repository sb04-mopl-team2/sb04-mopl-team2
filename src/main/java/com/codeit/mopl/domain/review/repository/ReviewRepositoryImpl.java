package com.codeit.mopl.domain.review.repository;

import com.codeit.mopl.domain.review.entity.QReview;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.base.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
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
      SortBy sortBy){

    QReview qReview = QReview.review;

    BooleanBuilder where = new BooleanBuilder();
    where.and(qReview.content.id.eq(contentId));
    where.and(qReview.isDeleted.eq(false));

    if (cursor != null && idAfter != null) {
      where.and(buildCursorCondition(cursor, idAfter, sortBy, sortDirection, qReview));
      where.and(qReview.id.notIn(idAfter));
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

  private List<OrderSpecifier<?>> buildOrderSpecifiers(
      SortBy sortBy,
      SortDirection sortDirection,
      QReview qReview
  ) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();

    if (sortBy != null && sortDirection != null) {
      Order order = (sortDirection == SortDirection.DESCENDING)
          ? Order.DESC
          : Order.ASC;

      switch (sortBy) {
        case CREATED_AT:
          orders.add(new OrderSpecifier<>(order, qReview.createdAt));
          break;

        case RATING:
          orders.add(new OrderSpecifier<>(order, qReview.rating));
          break;
      }

      orders.add(new OrderSpecifier<>(order, qReview.id));
    }

    return orders;
  }


  private BooleanExpression buildCursorCondition(
      String cursor,
      UUID idAfter,
      SortBy sortBy,
      SortDirection sortDirection,
      QReview qReview
  ) {

    if (sortBy == null || sortDirection == null) {
      return null;
    }

    BooleanExpression main;
    BooleanExpression tie;

    switch (sortBy) {

      case CREATED_AT: {
        Instant cursorInstant = Instant.parse(cursor);

        if (sortDirection == SortDirection.DESCENDING) {
          main = qReview.createdAt.lt(cursorInstant);
          tie = qReview.createdAt.eq(cursorInstant).and(qReview.id.lt(idAfter));
        } else {
          main = qReview.createdAt.gt(cursorInstant);
          tie = qReview.createdAt.eq(cursorInstant).and(qReview.id.gt(idAfter));
        }

        return main.or(tie);
      }

      case RATING: {
        double cursorRating = Double.parseDouble(cursor);

        if (sortDirection == SortDirection.DESCENDING) {
          main = qReview.rating.lt(cursorRating);
          tie = qReview.rating.eq(cursorRating).and(qReview.id.lt(idAfter));
        } else {
          main = qReview.rating.gt(cursorRating);
          tie = qReview.rating.eq(cursorRating).and(qReview.id.gt(idAfter));
        }

        return main.or(tie);
      }

      default:
        return null;
    }
  }
}
