package com.codeit.mopl.domain.watchingsession.repository;

import static com.codeit.mopl.domain.content.entity.QContent.content;
import static com.codeit.mopl.domain.user.entity.QUser.user;
import static com.codeit.mopl.domain.watchingsession.entity.QWatchingSession.watchingSession;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomWatchingSessionRepositoryImpl implements CustomWatchingSessionRepository {

  private final JPAQueryFactory jpaQueryFactory;

  @Override
  public List<WatchingSession> findWatchingSessions(
      UUID contentId,
      String watcherNameLike, // (optional)
      String cursor, // (optional) createdAt timestamp
      UUID idAfter, // (optional) ID of last item
      int limit,
      SortDirection sortDirection,
      SortBy sortBy // createdAt
  ) {
    log.info("[실시간 세션] 레포지토리에서 조회 시작. contentId = {} ", contentId);
    List<WatchingSession> results = jpaQueryFactory.selectFrom(watchingSession)
        .join(watchingSession.user, user).fetchJoin()
        .join(watchingSession.content, content).fetchJoin()
        .where(
            watchingSession.content.id.eq(contentId), // contentId match
            watcherNameExist(watcherNameLike), //filter -> if name exist
            cursorCondition(cursor, idAfter, sortDirection)
        )
        .orderBy(getSortOrder(sortDirection))
        .limit(limit)
        .fetch();

    log.info("[실시간 세션] 레포지토리에서 찾는 watchingsession 반환 완료. contentId = {}, 갯수 = {} ",
        contentId, results.size());
    return results;

  }

  private BooleanExpression cursorCondition(String cursor, UUID idAfter, SortDirection sortDirection) {

    // 1st page
    if (cursor == null || idAfter == null) return null;

    // only return according to the directions
    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    LocalDateTime lastCreatedAt = LocalDateTime.parse(cursor, formatter);

    if (sortDirection == SortDirection.ASCENDING) {
      // WHERE (createdAt > lastCreatedAt)
      //     OR (createdAt = lastCreatedAt AND id > lastId)
      return watchingSession.createdAt.gt(lastCreatedAt)
          .or(watchingSession.createdAt.eq(lastCreatedAt)
            .and(watchingSession.id.gt(idAfter)));
    } else {
      // WHERE (createdAt < lastCreatedAt)
      //     OR (createdAt = lastCreatedAt AND id < lastId)
      return watchingSession.createdAt.lt(lastCreatedAt)
          .or(watchingSession.createdAt.eq(lastCreatedAt)
            .and(watchingSession.id.lt(idAfter)));
    }
  }

  // sort by createdAt AND Id
  private OrderSpecifier<?>[] getSortOrder(SortDirection sortDirection) {
    Order order = (sortDirection == SortDirection.ASCENDING) ? Order.ASC : Order.DESC;
    OrderSpecifier<?> sortByCreatedAt = new OrderSpecifier<>(order, watchingSession.createdAt);
    OrderSpecifier<?> sortById = new OrderSpecifier<>(order, watchingSession.id);
    return new OrderSpecifier[]{sortByCreatedAt, sortById};
  }

  // returns watcherCount == totalCount
  @Override
  public long getWatcherCount(UUID contentId, String watcherNameLike) {
    Long count = jpaQueryFactory
        .select(watchingSession.count())
        .from(watchingSession)
        .where(
            watchingSession.content.id.eq(contentId),
            watcherNameExist(watcherNameLike) //
        )
        .fetchOne();
    return count != null ? count : 0L;
  }

  // boolean expressions for get watcherCount
  public BooleanExpression watcherNameExist(String watcherNameLike) {
    return watcherNameLike != null && !watcherNameLike.isBlank()
        ? watchingSession.user.name.containsIgnoreCase(watcherNameLike)
        : null;
  }
}
