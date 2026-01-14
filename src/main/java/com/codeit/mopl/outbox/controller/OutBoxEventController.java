package com.codeit.mopl.outbox.controller;


import com.codeit.mopl.outbox.dto.CursorResponseOutBoxEventDto;
import com.codeit.mopl.outbox.dto.OutBoxEventDto;
import com.codeit.mopl.outbox.dto.OutBoxSearchRequest;
import com.codeit.mopl.outbox.service.OutBoxEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        log.info("[OutBox] OutBox 이벤트 목록 조회 요청");
        CursorResponseOutBoxEventDto result = outBoxEventService.getOutBoxEvents(request);
        log.info("[OutBox] OutBox 이벤트 목록 조회 응답: totalCount = {}, hasNext = {}, nextCursor = {}",
                result.totalCount(), result.hasNext(), result.nextCursor());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{outboxEventId}/retry")
    public ResponseEntity<OutBoxEventDto> retryOutBoxEvent(@PathVariable("outboxEventId") UUID outboxEventId) {
        log.info("[OutBox] 특정 OutBox 이벤트 재시도 요청: outboxEventId = {}", outboxEventId);
        OutBoxEventDto result = outBoxEventService.retryFollowOutBoxEvent(outboxEventId);
        log.info("[OutBox] 특정 OutBox 이벤트 재시도 완료: outboxEventId = {}", outboxEventId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/retry")
    public ResponseEntity<List<OutBoxEventDto>> retryAllDeadOutBoxEvents(@RequestParam(defaultValue = "100") int limit) {
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 요청: limit = {}", limit);
        List<OutBoxEventDto> result = outBoxEventService.retryAllDeadOutBoxEvent(limit);
        log.info("[OutBox] DEAD 상태의 OutBox 이벤트 일괄 재시도 완료: totalCount = {}", result.size());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
