package com.codeit.mopl.outbox.controller;


import com.codeit.mopl.outbox.dto.*;
import com.codeit.mopl.outbox.service.OutBoxEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


/**
 * 운영 중 장애 대응을 위한 OutBox Admin API
 * - 일반 사용자 접근 불가
 * - 관리자 수동 재처리 용도
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/outbox")
public class OutBoxEventController {

    private final OutBoxEventService outBoxEventService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<CursorResponseOutBoxEventDto> getOutBoxEvents(@Valid OutBoxSearchRequest request) {
        log.info("[OutBox] OutBox 목록 조회 요청");
        CursorResponseOutBoxEventDto result = outBoxEventService.getOutBoxEvents(request);
        log.info("[OutBox] OutBox 목록 조회 응답: totalCount = {}, hasNext = {}, nextCursor = {}",
                result.totalCount(), result.hasNext(), result.nextCursor());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{outboxEventId}/retry")
    public ResponseEntity<OutBoxEventDto> retryDeadOutBoxEvent(@PathVariable("outboxEventId") UUID outboxEventId) {
        log.info("[OutBox] DEAD 상태의 특정 OutBox 재시도 요청: outboxEventId = {}", outboxEventId);
        OutBoxEventDto result = outBoxEventService.resetDeadOutBoxEventToRequested(outboxEventId);
        log.info("[OutBox] DEAD 상태의 특정 OutBox 재시도 완료: outboxEventId = {}", outboxEventId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/retry")
    public ResponseEntity<DeadOutBoxEventsRetryDto> retryDeadOutBoxEvents(@RequestBody DeadOutBoxEventsRetryRequest request) {
        log.info("[OutBox] DEAD 상태의 OutBox 일괄 재시도 요청: request = {}", request);
        DeadOutBoxEventsRetryDto result = outBoxEventService.resetDeadOutBoxEventsToRequested(request);
        log.info("[OutBox] DEAD 상태의 OutBox 일괄 재시도 완료: result = {}", result);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{outboxEventId}")
    public ResponseEntity<Void> deleteOutBoxEvent(@PathVariable("outboxEventId") UUID outboxEventId) {
        log.info("[OutBox] OutBox 삭제 요청: outboxEventId = {}", outboxEventId);
        outBoxEventService.deleteOutBoxEvent(outboxEventId);
        log.info("[OutBox] OutBox 삭제 완료: outboxEventId = {}", outboxEventId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
