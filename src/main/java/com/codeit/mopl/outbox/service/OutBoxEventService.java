package com.codeit.mopl.outbox.service;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.exception.outbox.OutBoxEventNotFoundException;
import com.codeit.mopl.outbox.dto.OutBoxEventDto;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.mapper.OutBoxEventMapper;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
import com.codeit.mopl.outbox.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public OutBoxEventDto createOutBoxEvent(EventType eventType, String aggregateType, UUID aggregateId, Object domainEvent) {
        log.info("[OutBox] OutBox 이벤트 생성 시작");
        String payload = serializer.serialize(domainEvent);
        OutBoxEvent event = new OutBoxEvent(eventType,aggregateType, aggregateId, payload);
        outBoxEventRepository.save(event);
        OutBoxEventDto result = outBoxEventMapper.toDto(event);
        log.info("[OutBox] OutBox 이벤트 생성 완료");
        return result;
    }

    @Transactional(readOnly = true)
    public List<OutBoxEventDto> getDeadFollowOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 시작");
        List<OutBoxEventDto> result = outBoxEventRepository.findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.DEAD, PageRequest.of(0, 1000))
                .stream()
                .map(outBoxEventMapper::toDto)
                .toList();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 완료: totalCount = {}", result.size());
        return result;
    }

    @Transactional
    public OutBoxEventDto retryFollowOutBoxEvent(UUID outBoxEventId) {
        log.info("[OutBox] OutBox 이벤트 재시도 횟수 초기화 시작: outBoxId = {}", outBoxEventId);
        OutBoxEvent event = outBoxEventRepository.findById(outBoxEventId)
                .orElseThrow(() -> OutBoxEventNotFoundException.withId(outBoxEventId));
        event.markRequested();
        OutBoxEventDto result = outBoxEventMapper.toDto(event);
        log.info("[OutBox] OutBox 이벤트 재시도 횟수 초기화 완료: outBoxId = {}, retryCount = {}, status = {}", outBoxEventId, result.retryCount(), result.outBoxStatus());
        return result;
    }

    @Transactional
    public List<OutBoxEventDto> retryAllDeadOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 횟수 초기화 시작");
        List<OutBoxEvent> events = outBoxEventRepository.findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.DEAD, PageRequest.of(0, 100));
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
