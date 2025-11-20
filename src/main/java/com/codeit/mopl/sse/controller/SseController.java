package com.codeit.mopl.sse.controller;

import com.codeit.mopl.security.CustomUserDetails;
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
      @AuthenticationPrincipal CustomUserDetails user,
      @RequestParam(value = "LastEventId", required = false) UUID lastEventId
  ) {

    log.info("[SSE] SSE 연결 요청 시작, receiverId = {}, lastEventId = {}", user.getUser().id(), lastEventId);
    UUID receiverId = user.getUser().id();

    SseEmitter emitter = sseService.connect(receiverId, lastEventId);

    log.info("[SSE] SSE 연결 요청 종료, receiverId = {}", receiverId);
    return emitter;
  }
}
