package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.dto.DeadOutBoxEventsRetryRequest;
import com.codeit.mopl.outbox.dto.OutBoxSearchRequest;
import com.codeit.mopl.outbox.entity.*;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomOutBoxEventRepositoryImpl implements CustomOutBoxEventRepository {

    private final JPAQueryFactory query;
    private final QOutBoxEvent outbox = QOutBoxEvent.outBoxEvent;

    @Override
    public List<OutBoxEvent> findByCursor(OutBoxSearchRequest request) {
        List<OrderSpecifier<?>> orders = buildOrderSpecification(request.sortBy(), request.sortDirection());

        return query.selectFrom(outbox)
                .where(
                        eventTypeEq(request.eventType()),
                        aggregateTypeEq(request.aggregateType()),
                        outBoxStatusEq(request.outBoxStatus()),
                        retryCountEq(request.retryCount()),
                        lastErrorMessageContains(request.lastErrorMessage()),
                        createdAtBetween(request.createdFrom(), request.createdTo()),
                        buildCursorCondition(request.cursor(), request.idAfter(), request.sortBy(), request.sortDirection())
                )
                .orderBy(orders.toArray(OrderSpecifier[]::new))
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

    private BooleanExpression buildCursorCondition(String cursor, UUID idAfter, OutBoxSortBy outBoxSortBy, SortDirection sortDirection) {
        if (cursor == null || idAfter == null) {
            return null;
        }

        OutBoxSortBy sortBy = outBoxSortBy != null ? outBoxSortBy : OutBoxSortBy.CREATED_AT;
        SortDirection direction = sortDirection != null ? sortDirection : SortDirection.ASCENDING;
        Order order = direction == SortDirection.ASCENDING ? Order.ASC : Order.DESC;

        return switch (sortBy) {
            case CREATED_AT -> {
                Instant cursorInstant;
                try {
                    cursorInstant = Instant.parse(cursor);
                } catch (DateTimeException e) {
                    throw new IllegalArgumentException("올바르지 않은 커서 포맷입니다: " + cursor, e);
                }

                yield order == Order.ASC
                        ? outbox.createdAt.gt(cursorInstant)
                        .or(outbox.createdAt.eq(cursorInstant).and(outbox.id.gt(idAfter)))
                        : outbox.createdAt.lt(cursorInstant)
                        .or(outbox.createdAt.eq(cursorInstant).and(outbox.id.lt(idAfter)));
            }

            case RETRY_COUNT -> {
                int retryCountCursor;
                try {
                    retryCountCursor = Integer.parseInt(cursor);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("올바르지 않은 커서 포맷입니다:  " + cursor, e);
                }
                yield order == Order.ASC
                        ? outbox.retryCount.gt(retryCountCursor)
                        .or(outbox.retryCount.eq(retryCountCursor).and(outbox.id.gt(idAfter)))
                        : outbox.retryCount.lt(retryCountCursor)
                        .or(outbox.retryCount.eq(retryCountCursor).and(outbox.id.lt(idAfter)));
            }
        };
    }

    private List<OrderSpecifier<?>> buildOrderSpecification(OutBoxSortBy outBoxSortBy, SortDirection sortDirection) {
        // 정렬 조건 디폴트 값: CREATED_AT, 정렬 방향 디폴트 값: ASCENDING
        OutBoxSortBy sortBy = outBoxSortBy != null ? outBoxSortBy : OutBoxSortBy.CREATED_AT;
        SortDirection direction = sortDirection != null ? sortDirection : SortDirection.ASCENDING;
    
        Order order = direction == SortDirection.ASCENDING
                ? Order.ASC
                : Order.DESC;
        
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        
        switch (sortBy) {
            case CREATED_AT -> orders.add(new OrderSpecifier<>(order, outbox.createdAt));
            case RETRY_COUNT -> orders.add(new OrderSpecifier<>(order, outbox.retryCount));
        }
        // id 보조 정렬 추가
        orders.add(new OrderSpecifier<>(order, outbox.id));
        return orders;
    }

    private int resolveLimit(Integer limit) {
        // limit 디폴트 값: 500
        return limit != null ? limit : 500;
    }
}
