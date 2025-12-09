package com.codeit.mopl.domain.watchingsession.repository;

import static com.codeit.mopl.domain.content.entity.QContent.content;
import static com.codeit.mopl.domain.user.entity.QUser.user;
import static com.codeit.mopl.domain.watchingsession.entity.QWatchingSession.watchingSession;

import com.codeit.mopl.domain.base.TimeUtil;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
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
      String cursor, // (optional) createdAt 타임스탬프
      UUID idAfter, // (optional) 마지막 아이템 UUID
      int limit,
      SortDirection sortDirection,
      SortBy sortBy // createdAt
  ) {
    log.info("[실시간 세션] 레포지토리에서 조회 시작. contentId = {} ", contentId);
    List<WatchingSession> results = jpaQueryFactory.selectFrom(watchingSession)
        .join(watchingSession.user, user).fetchJoin()
        .join(watchingSession.content, content).fetchJoin()
        .where(
            watchingSession.content.id.eq(contentId), // 컨텐츠 아이디 필터
            watcherNameExist(watcherNameLike), // watcherNameLike로 필터
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

    // 첫번째 페이지 빠른 리턴
    if (cursor == null || idAfter == null) return null;

    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    LocalDateTime lastCreatedAt = LocalDateTime.parse(cursor, formatter);
    Instant cursorInstant = TimeUtil.toInstant(lastCreatedAt);

    if (sortDirection == SortDirection.ASCENDING) {
      // WHERE (createdAt > lastCreatedAt)
      //     OR (createdAt = lastCreatedAt AND id > lastId)
      return watchingSession.createdAt.gt(cursorInstant)
          .or(watchingSession.createdAt.eq(cursorInstant)
            .and(watchingSession.id.gt(idAfter)));
    } else {
      // WHERE (createdAt < lastCreatedAt)
      //     OR (createdAt = lastCreatedAt AND id < lastId)
      return watchingSession.createdAt.lt(cursorInstant)
          .or(watchingSession.createdAt.eq(cursorInstant)
            .and(watchingSession.id.lt(idAfter)));
    }
  }

  // 정렬 createdAt AND Id
  private OrderSpecifier<?>[] getSortOrder(SortDirection sortDirection) {
    Order order = (sortDirection == SortDirection.ASCENDING) ? Order.ASC : Order.DESC;
    OrderSpecifier<?> sortByCreatedAt = new OrderSpecifier<>(order, watchingSession.createdAt);
    OrderSpecifier<?> sortById = new OrderSpecifier<>(order, watchingSession.id);
    return new OrderSpecifier[]{sortByCreatedAt, sortById};
  }

  @Override
  public long getWatcherCount(UUID contentId, String watcherNameLike) {
    Long count = jpaQueryFactory
        .select(watchingSession.count())
        .from(watchingSession)
        .where(
            watchingSession.content.id.eq(contentId),
            watcherNameExist(watcherNameLike)
        )
        .fetchOne();
    return count != null ? count : 0L;
  }

  public BooleanExpression watcherNameExist(String watcherNameLike) {
    return watcherNameLike != null && !watcherNameLike.isBlank()
        ? watchingSession.user.name.containsIgnoreCase(watcherNameLike)
        : null;
  }
}
