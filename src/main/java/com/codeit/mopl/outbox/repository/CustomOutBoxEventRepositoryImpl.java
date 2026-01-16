package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.dto.DeadOutBoxEventsRetryRequest;
import com.codeit.mopl.outbox.dto.OutBoxSearchRequest;
import com.codeit.mopl.outbox.entity.*;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomOutBoxEventRepositoryImpl implements CustomOutBoxEventRepository {

    private final JPAQueryFactory query;
    private final QOutBoxEvent outbox = QOutBoxEvent.outBoxEvent;

    @Override
    public List<OutBoxEvent> findByCursor(OutBoxSearchRequest request) {
        return query.selectFrom(outbox)
                .where(
                        eventTypeEq(request.eventType()),
                        aggregateTypeEq(request.aggregateType()),
                        outBoxStatusEq(request.outBoxStatus()),
                        retryCountEq(request.retryCount()),
                        lastErrorMessageContains(request.lastErrorMessage()),
                        createdAtBetween(request.createdFrom(), request.createdTo()),
                        buildCursorCondition(request.cursor(), request.idAfter(), request.sortDirection())
                )
                .orderBy(buildOrderBy(request.sortBy(), request.sortDirection()))
                .limit(resolveLimit(request.limit()) + 1)
                .fetch();
    }

    @Override
    public List<OutBoxEvent> findDeadOutBoxEventsByConditions(DeadOutBoxEventsRetryRequest request) {
        return query.selectFrom(outbox)
                .where(
                        eventTypeEq(request.eventType()),
                        aggregateTypeEq(request.aggregateType()),
                        outBoxStatusEq(OutBoxStatus.DEAD),
                        createdAtBetween(request.createdFrom(), request.createdTo())
                )
                .orderBy(outbox.createdAt.asc())
                .limit(resolveLimit(request.limit()))
                .fetch();
    }

    private BooleanExpression eventTypeEq(EventType eventType) {
        return eventType != null ? outbox.eventType.eq(eventType) : null;
    }

    private BooleanExpression aggregateTypeEq(AggregateType aggregateType) {
        return aggregateType != null ? outbox.aggregateType.eq(aggregateType) : null;
    }

    private BooleanExpression outBoxStatusEq(OutBoxStatus outBoxStatus) {
        return outBoxStatus != null ? outbox.outBoxStatus.eq(outBoxStatus) : null;
    }

    private BooleanExpression retryCountEq(Integer retryCount) {
        return retryCount != null ? outbox.retryCount.eq(retryCount) : null;
    }

    private BooleanExpression lastErrorMessageContains(String lastErrorMessage) {
        return StringUtils.isNotBlank(lastErrorMessage) ? outbox.lastErrorMessage.contains(lastErrorMessage) : null;
    }

    private BooleanExpression createdAtBetween(LocalDate createdFrom, LocalDate createdTo) {
        if (createdFrom == null && createdTo == null) return null;

        ZoneId zoneId = ZoneOffset.UTC;

        if (createdFrom != null && createdTo != null) {
            Instant from = createdFrom.atStartOfDay(zoneId).toInstant();
            Instant to = createdTo.plusDays(1).atStartOfDay(zoneId).toInstant();
            return outbox.createdAt.between(from, to);
        } else if (createdFrom != null) {
            Instant from = createdFrom.atStartOfDay(zoneId).toInstant();
            return outbox.createdAt.goe(from);
        } else {
            // createdTo만 존재
            Instant to = createdTo.plusDays(1).atStartOfDay(zoneId).toInstant();
            return outbox.createdAt.loe(to);
        }
    }

    private BooleanExpression buildCursorCondition(String cursor, UUID idAfter, SortDirection sortDirection) {
        if (cursor == null || idAfter == null) {
            return null;
        }
        Instant cursorInstant;
        try {
            cursorInstant = Instant.parse(cursor);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("올바르지 않은 커서 포맷입니다: " + cursor, e);
        }

        if (sortDirection == SortDirection.DESCENDING) {
            return outbox.createdAt.lt(cursorInstant)
                    .or(outbox.createdAt.eq(cursorInstant).and(outbox.id.lt(idAfter)));
        } else {
            // ASCENDING
            return outbox.createdAt.gt(cursorInstant)
                    .or(outbox.createdAt.eq(cursorInstant).and(outbox.id.gt(idAfter)));
        }
    }

    private OrderSpecifier<?> buildOrderBy(OutBoxSortBy outBoxSortBy, SortDirection sortDirection) {
        // 정렬 조건 디폴트 값: CREATED_AT, 정렬 방향 디폴트 값: ASCENDING
        OutBoxSortBy sortBy = outBoxSortBy != null ? outBoxSortBy : OutBoxSortBy.CREATED_AT;
        SortDirection direction = sortDirection != null ? sortDirection : SortDirection.ASCENDING;

        return switch (sortBy) {
            case CREATED_AT -> direction == SortDirection.ASCENDING
                    ? outbox.createdAt.asc()
                    : outbox.createdAt.desc();
            case RETRY_COUNT -> direction == SortDirection.ASCENDING
                    ? outbox.retryCount.asc()
                    : outbox.retryCount.desc();
        };
    }

    private int resolveLimit(Integer limit) {
        // limit 디폴트 값: 500
        return limit != null ? limit : 500;
    }
}
