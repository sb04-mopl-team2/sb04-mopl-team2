package com.codeit.mopl.outbox.service;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.exception.outbox.OutBoxEventNotFoundException;
import com.codeit.mopl.exception.outbox.OutBoxEventRetryNotAllowedException;
import com.codeit.mopl.outbox.dto.*;
import com.codeit.mopl.outbox.entity.AggregateType;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxSortBy;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.mapper.OutBoxEventMapper;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
import com.codeit.mopl.outbox.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutBoxEventService {

    private final OutBoxEventRepository outBoxEventRepository;
    private final OutBoxEventMapper outBoxEventMapper;
    private final EventSerializer serializer;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OutBoxEventDto createOutBoxEvent(OutBoxEventCreateRequest request) {
        log.info("[OutBox] OutBox 생성 시작: request = {}", request);
        String payload = serializer.serialize(request.domainEvent(), request.eventClassName());
        OutBoxEvent event = new OutBoxEvent(request.eventType(), request.aggregateType(), request.aggregateId(), payload);
        outBoxEventRepository.save(event);
        OutBoxEventDto result = outBoxEventMapper.toDto(event);
        eventPublisher.publishEvent(event);
        log.info("[OutBox] OutBox 생성 완료: id = {}", result.id());
        return result;
    }

    @Transactional(readOnly = true)
    public CursorResponseOutBoxEventDto getOutBoxEvents(OutBoxSearchRequest request) {
        log.info("[OutBox] OutBox 목록 조회 시작: request = {}", request);
        List<OutBoxEvent> outBoxEventList = outBoxEventRepository.findByCursor(request);

        // 정렬 조건 디폴트 값: CREATED_AT, 정렬 방향 디폴트 값: ASCENDING
        OutBoxSortBy sortBy = request.sortBy() != null ? request.sortBy() : OutBoxSortBy.CREATED_AT;
        SortDirection sortDirection = request.sortDirection() != null ? request.sortDirection() : SortDirection.ASCENDING;

        if (outBoxEventList.isEmpty()) {
            log.info("[OutBox] OutBox 목록 조회 완료: 결과 없음");
            return new CursorResponseOutBoxEventDto(
                    new ArrayList<>(),
                    null,
                    null,
                    false,
                    0L,
                    sortBy,
                    sortDirection
            );
        }

        // limit이 null이면 디폴트 값인 500이 적용됨
        int limit = request.limit() != null ? request.limit() : 500;
        boolean hasNext = outBoxEventList.size() > limit;
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext) {
            // hasNext가 true면 1만큼 더 조회되었으므로 초과 부분 자르기
            outBoxEventList = outBoxEventList.subList(0, limit);
            OutBoxEvent lastOutBoxEvent = outBoxEventList.get(outBoxEventList.size() - 1);

            switch (sortBy) {
                case CREATED_AT -> nextCursor = lastOutBoxEvent.getCreatedAt().toString();
                case RETRY_COUNT -> {
                    Integer retryCount = lastOutBoxEvent.getRetryCount();
                    nextCursor = retryCount.toString();
                }
            }
            nextIdAfter = lastOutBoxEvent.getId();
        }

        List<OutBoxEventDto> data = outBoxEventList.stream()
                .map(outBoxEventMapper::toDto)
                .toList();

        long currentPageCount = data.size();

        CursorResponseOutBoxEventDto result = new CursorResponseOutBoxEventDto(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                currentPageCount,
                sortBy,
                sortDirection
        );
        log.info("[OutBox] OutBox 목록 조회 완료: currentPageCount = {}", currentPageCount);
        return result;
    }

    @Transactional
    public OutBoxEventDto resetDeadOutBoxEventToRequested(UUID outBoxEventId) {
        log.info("[OutBox] DEAD 상태의 OutBox 초기화 시작: outBoxId = {}", outBoxEventId);
        OutBoxEvent event = outBoxEventRepository.findById(outBoxEventId)
                .orElseThrow(() -> OutBoxEventNotFoundException.withId(outBoxEventId));

        // DEAD 상태가 아닌 OutBoxEvent는 재시도 횟수를 초기화할 수 없음
        OutBoxStatus status = event.getOutBoxStatus();
        if (status != OutBoxStatus.DEAD) {
            throw OutBoxEventRetryNotAllowedException.withIdAndStatus(outBoxEventId, status);
        }

        event.markRequested();
        OutBoxEventDto result = outBoxEventMapper.toDto(event);
        log.info("[OutBox] DEAD 상태의 OutBox 초기화 완료: outBoxId = {}, retryCount = {}, status = {}", outBoxEventId, result.retryCount(), result.outBoxStatus());
        return result;
    }

    @Transactional
    public DeadOutBoxEventsRetryDto resetDeadOutBoxEventsToRequested(DeadOutBoxEventsRetryRequest request) {
        log.info("[OutBox] DEAD 상태의 OutBox 일괄 초기화 시작: request = {}", request);
        // limit이 null일 시 디폴트 값: 500
        List<OutBoxEvent> outBoxEventList = outBoxEventRepository.findDeadOutBoxEventsByConditions(request);

        if (outBoxEventList.isEmpty()) {
            log.info("[OutBox] DEAD 상태의 OutBox 일괄 초기화 중단: 해당 조건에 맞는 OutBox가 없습니다.");
            return new DeadOutBoxEventsRetryDto(
                    0,
                    new HashSet<>(),
                    new HashSet<>(),
                    null,
                    null,
                    Instant.now()
            );
        }

        for (OutBoxEvent event : outBoxEventList) {
            event.markRequested();
        }

        Set<EventType> eventTypes = outBoxEventList.stream()
                .map(OutBoxEvent::getEventType)
                .collect(Collectors.toSet());

        Set<AggregateType> aggregateTypes = outBoxEventList.stream()
                .map(OutBoxEvent::getAggregateType)
                .collect(Collectors.toSet());
        
        // 일괄 초기화는 무조건 ASC 정렬
        Instant createdFrom = outBoxEventList.get(0).getCreatedAt();
        Instant createdTo = outBoxEventList.get(outBoxEventList.size() - 1).getCreatedAt();

        DeadOutBoxEventsRetryDto result = new DeadOutBoxEventsRetryDto(
                outBoxEventList.size(),
                eventTypes,
                aggregateTypes,
                createdFrom,
                createdTo,
                Instant.now()
        );
        log.info("[OutBox] DEAD 상태의 OutBox 일괄 초기화 완료: totalCount = {}", result.totalCount());
        return result;
    }

    @Transactional
    public void deleteOutBoxEvent(UUID outBoxEventId) {
        log.info("[OutBox] OutBox 삭제 시작: outBoxEventId = {}", outBoxEventId);

        if (!outBoxEventRepository.existsById(outBoxEventId)) {
            log.info("[OutBox] 해당 id를 가진 OutBox가 없습니다: outBoxEventId = {}", outBoxEventId);
            throw OutBoxEventNotFoundException.withId(outBoxEventId);
        }

        outBoxEventRepository.deleteById(outBoxEventId);
        log.info("[OutBox] OutBox 삭제 완료: outBoxEventId = {}", outBoxEventId);
    }
}
