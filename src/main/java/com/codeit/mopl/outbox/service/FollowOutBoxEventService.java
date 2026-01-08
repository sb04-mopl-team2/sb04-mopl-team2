package com.codeit.mopl.outbox.service;

import com.codeit.mopl.outbox.dto.FollowOutBoxEventDto;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.mapper.FollowOutBoxEventMapper;
import com.codeit.mopl.outbox.repository.FollowOutBoxEventRepository;
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
public class FollowOutBoxEventService {

    private final FollowOutBoxEventRepository followOutBoxEventRepository;
    private final FollowOutBoxEventMapper followOutBoxEventMapper;

    @Transactional(readOnly = true)
    public List<FollowOutBoxEventDto> getDeadFollowOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 시작");
        List<FollowOutBoxEventDto> result = followOutBoxEventRepository.findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.DEAD, PageRequest.of(0, 1000))
                .stream()
                .map(followOutBoxEventMapper::toDto)
                .toList();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 완료: totalCount = {}", result.size());
        return result;
    }

    @Transactional
    public FollowOutBoxEventDto retryFollowOutBoxEvent(UUID followOutboxEventId) {
        log.info("[OutBox] OutBox 이벤트 재시도 횟수 초기화 시작: outBoxId = {}", followOutboxEventId);
        FollowOutBoxEvent event =  followOutBoxEventRepository.findById(followOutboxEventId)
                .orElseThrow(() -> new RuntimeException("FollowOutBoxEvent not found"));
        event.markRequested();
        FollowOutBoxEventDto result = followOutBoxEventMapper.toDto(event);
        log.info("[OutBox] OutBox 이벤트 재시도 횟수 초기화 완료: outBoxId = {}, retryCount = {}, status = {}", followOutboxEventId, result.retryCount(), result.outBoxStatus());
        return result;
    }
    
    @Transactional
    public List<FollowOutBoxEventDto> retryAllDeadOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 횟수 초기화 시작");
        List<FollowOutBoxEvent> events = followOutBoxEventRepository.findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.DEAD, PageRequest.of(0, 100));
        for (FollowOutBoxEvent event : events) {
            event.markRequested();
        }
        List<FollowOutBoxEventDto> result = events.stream()
                .map(followOutBoxEventMapper::toDto)
                .toList();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 횟수 초기화 완료: totalCount = {}", result.size());
        return result;
    }
}
