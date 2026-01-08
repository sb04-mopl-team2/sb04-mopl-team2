package com.codeit.mopl.outbox.controller;


import com.codeit.mopl.outbox.dto.FollowOutBoxEventDto;
import com.codeit.mopl.outbox.service.FollowOutBoxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/follows/outbox")
public class FollowOutBoxEventController {

    private final FollowOutBoxEventService followOutBoxEventService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dead")
    public ResponseEntity<List<FollowOutBoxEventDto>> getDeadFollowOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 요청");
        List<FollowOutBoxEventDto> result = followOutBoxEventService.getDeadFollowOutBoxEvent();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 응답: totalCount = {}", result.size());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{followOutboxEventId}/retry")
    public ResponseEntity<FollowOutBoxEventDto> retryFollowOutBoxEvent(@PathVariable("followOutboxEventId") UUID followOutboxEventId) {
        log.info("[OutBox] 특정 OutBoxEvent 재시도 요청: followOutboxEventId = {}", followOutboxEventId);
        FollowOutBoxEventDto result = followOutBoxEventService.retryFollowOutBoxEvent(followOutboxEventId);
        log.info("[OutBox] 특정 OutBoxEvent 재시도 완료: followOutboxEventId = {}", followOutboxEventId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/retry")
    public ResponseEntity<List<FollowOutBoxEventDto>> retryAllDeadFollowOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 요청");
        List<FollowOutBoxEventDto> result = followOutBoxEventService.retryAllDeadOutBoxEvent();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 완료");
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
