package com.codeit.mopl.outbox.service;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.exception.outbox.OutBoxEventNotFoundException;
import com.codeit.mopl.exception.outbox.OutBoxEventRetryNotAllowedException;
import com.codeit.mopl.outbox.dto.CursorResponseOutBoxEventDto;
import com.codeit.mopl.outbox.dto.OutBoxEventCreateRequest;
import com.codeit.mopl.outbox.dto.OutBoxEventDto;
import com.codeit.mopl.outbox.dto.OutBoxSearchRequest;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxSortBy;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.mapper.OutBoxEventMapper;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
import com.codeit.mopl.outbox.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutBoxEventService {

    private final OutBoxEventRepository outBoxEventRepository;
    private final OutBoxEventMapper outBoxEventMapper;
    private final EventSerializer serializer;

    @Transactional
    public OutBoxEventDto createOutBoxEvent(OutBoxEventCreateRequest request) {
        log.info("[OutBox] OutBox 이벤트 생성 시작");
        String payload = serializer.serialize(request.domainEvent());
        OutBoxEvent event = new OutBoxEvent(request.eventType(), request.aggregateType(), request.aggregateId(), payload);
        outBoxEventRepository.save(event);
        OutBoxEventDto result = outBoxEventMapper.toDto(event);
        log.info("[OutBox] OutBox 이벤트 생성 완료");
        return result;
    }

    @Transactional(readOnly = true)
    public CursorResponseOutBoxEventDto getOutBoxEvents(OutBoxSearchRequest request) {
        log.info("[OutBox] OutBox 이벤트 목록 조회 시작: request = {}", request);
        List<OutBoxEvent> outBoxEventList = outBoxEventRepository.findByCursor(request);

        if (outBoxEventList.isEmpty()) {
            log.info("[OutBox] OutBox 이벤트 목록 조회 완료: 결과 없음");
            return new CursorResponseOutBoxEventDto(
                    new ArrayList<>(),
                    null,
                    null,
                    false,
                    0L,
                    request.sortBy(),
                    request.sortDirection()
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
            nextCursor = lastOutBoxEvent.getCreatedAt().toString();
            nextIdAfter = lastOutBoxEvent.getId();
        }

        List<OutBoxEventDto> data = outBoxEventList.stream()
                .map(outBoxEventMapper::toDto)
                .toList();

        long totalCount = data.size();
        // 정렬 조건 디폴트 값: CREATED_AT, 정렬 방향 디폴트 값: ASCENDING
        OutBoxSortBy sortBy = request.sortBy() != null ? request.sortBy() : OutBoxSortBy.CREATED_AT;
        SortDirection sortDirection = request.sortDirection() != null ? request.sortDirection() : SortDirection.ASCENDING;

        CursorResponseOutBoxEventDto result = new CursorResponseOutBoxEventDto(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                sortBy,
                sortDirection
        );
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 완료: totalCount = {}", totalCount);
        return result;
    }

    @Transactional
    public OutBoxEventDto retryFollowOutBoxEvent(UUID outBoxEventId) {
        log.info("[OutBox] OutBox 이벤트 재시도 횟수 초기화 시작: outBoxId = {}", outBoxEventId);
        OutBoxEvent event = outBoxEventRepository.findById(outBoxEventId)
                .orElseThrow(() -> OutBoxEventNotFoundException.withId(outBoxEventId));

        // DEAD 상태가 아닌 OutBoxEvent는 재시도 횟수를 초기화할 수 없음
        OutBoxStatus status = event.getOutBoxStatus();
        if (status != OutBoxStatus.DEAD) {
            throw OutBoxEventRetryNotAllowedException.withIdAndStatus(outBoxEventId, status);
        }

        event.markRequested();
        OutBoxEventDto result = outBoxEventMapper.toDto(event);
        log.info("[OutBox] OutBox 이벤트 재시도 횟수 초기화 완료: outBoxId = {}, retryCount = {}, status = {}", outBoxEventId, result.retryCount(), result.outBoxStatus());
        return result;
    }

    @Transactional
    public List<OutBoxEventDto> retryAllDeadOutBoxEvent(int limit) {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 횟수 초기화 시작");
        // 최대치: 500
        List<OutBoxEvent> events = outBoxEventRepository.findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.DEAD, PageRequest.of(0, Math.min(limit, 500)));
        for (OutBoxEvent event : events) {
            event.markRequested();
        }
        List<OutBoxEventDto> result = events.stream()
                .map(outBoxEventMapper::toDto)
                .toList();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 횟수 초기화 완료: totalCount = {}", result.size());
        return result;
    }
}
