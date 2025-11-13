package com.codeit.mopl.sse.controller;

import com.codeit.mopl.sse.service.SseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SseController {

  private final SseService sseService;

  @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(
      @AuthenticationPrincipal UserDetails user,
      @RequestParam(value = "LastEventId", required = false) UUID lastEventId
  ) {
    // 인증 안 된 경우 방어
    if (user == null) {
      log.warn("SSE connect called without authenticated user");
      throw new RuntimeException("Unauthenticated SSE connection");
    }

    // TODO: 추후 UserDetailService와 연동하여 receiverId를 가져오도록 수정
    //UUID receiverId = user.getUserDto().id();
    UUID receiverId = UUID.randomUUID(); // 임시로 쓰는 랜덤 uuid

    log.info("SSE connect start: receiverId={}, lastEventId={}", receiverId, lastEventId);

    SseEmitter emitter = sseService.connect(receiverId, lastEventId);

    log.info("SSE connect success: receiverId={}, lastEventId={}", receiverId, lastEventId);
    return emitter;
  }
}
