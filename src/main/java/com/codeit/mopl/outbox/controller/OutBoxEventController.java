package com.codeit.mopl.outbox.controller;


import com.codeit.mopl.outbox.dto.OutBoxEventDto;
import com.codeit.mopl.outbox.service.OutBoxEventService;
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
@RequestMapping("/api/outbox")
public class OutBoxEventController {

    private final OutBoxEventService outBoxEventService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dead")
    public ResponseEntity<List<OutBoxEventDto>> getDeadOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 요청");
        List<OutBoxEventDto> result = outBoxEventService.getDeadFollowOutBoxEvent();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 목록 조회 응답: totalCount = {}", result.size());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{outboxEventId}/retry")
    public ResponseEntity<OutBoxEventDto> retryOutBoxEvent(@PathVariable("outboxEventId") UUID outboxEventId) {
        log.info("[OutBox] 특정 OutBox 이벤트 재시도 요청: outboxEventId = {}", outboxEventId);
        OutBoxEventDto result = outBoxEventService.retryFollowOutBoxEvent(outboxEventId);
        log.info("[OutBox] 특정 OutBox 이벤트 재시도 완료: outboxEventId = {}", outboxEventId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/retry")
    public ResponseEntity<List<OutBoxEventDto>> retryAllDeadOutBoxEvent() {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 요청");
        List<OutBoxEventDto> result = outBoxEventService.retryAllDeadOutBoxEvent();
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 완료");
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
