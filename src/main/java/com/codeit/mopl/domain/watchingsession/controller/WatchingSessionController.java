package com.codeit.mopl.domain.watchingsession.controller;

import com.codeit.mopl.domain.watchingsession.WatchingSessionService;
import com.codeit.mopl.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionRequest;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
   시청 세션 데이터 조회용 컨트롤러
   - 실제 시청 세션 데이터는 WebSocketEventListener이 수정
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WatchingSessionController {

  private final WatchingSessionService watchingSessionService;

  // 특정 사용자의 시청 목록 조회 (nullable)
  @GetMapping("/users/{watcherId}/watching-sessions")
  public ResponseEntity<WatchingSessionDto> getWatchingSessionPerUser(
      @PathVariable UUID watcherId
  ) {
    log.info("[실시간 세션] 특정 사용자 시청 목록 요청 수신. userId = {}", watcherId);
    WatchingSessionDto response = watchingSessionService.getByUserId(watcherId);
    log.info("[실시간 세션] 특정 사용자 시청 목록 응답 반환. userId = {}", watcherId);
    return ResponseEntity.ok(response);
  }

  // 특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)
  @GetMapping("/contents/{contentId}/watching-sessions")
  public ResponseEntity<CursorResponseWatchingSessionDto> getWatchingSessionsPerContent(
      @PathVariable UUID contentId,
      @Valid WatchingSessionRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    log.info("[실시간 세션] 특정 콘텐츠의 시청 세션 목록 조회 시작. contentId = {}", contentId);
    CursorResponseWatchingSessionDto response = watchingSessionService.getWatchingSessions(
        userDetails.getUser().id(),
        contentId,
        request.watcherNameLike(),
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortDirection(),
        request.sortBy()
    );
    log.info("[실시간 세션] 특정 콘텐츠의 시청 세션 목록 조회 완료. contentId = {}", contentId);
    return ResponseEntity.ok(response);
  }

}
