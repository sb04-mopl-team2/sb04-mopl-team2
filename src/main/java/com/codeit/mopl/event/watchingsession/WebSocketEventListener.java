package com.codeit.mopl.event.watchingsession;

import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.mopl.domain.watchingsession.service.WatchingSessionService;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.exception.watchingsession.UserNotAuthenticatedException;
import com.codeit.mopl.exception.watchingsession.WatchingSessionErrorCode;
import com.codeit.mopl.domain.watchingsession.service.RedisPublisher;
import com.codeit.mopl.security.CustomUserDetails;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final WatchingSessionService service;
  private final UserRepository userRepository;
  private final WatchingSessionRepository watchingSessionRepository;
//  private final SimpMessagingTemplate messagingTemplate;
  private final RedisPublisher redisPublisher;

  /*
     콘텐츠 시청 세션: 누가 시청 세션에 들어오고 나가는지 (참가자 목록) 업데이트를 받기 위해
     - 엔드포인트: SUBSCRIBE /sub/contents/{contentId}/watch
     - 페이로드: WatchingSessionChange
   */
  @EventListener
  public void handleSessionSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    String destination = accessor.getDestination();

    log.info("[WebsocketEventListener] handleSessionSubscribe 시작 - sessionId: {}, destination: {}", sessionId, destination);

    if (destination == null) {
      return;
    }

    String contentId;
    if (destination.startsWith("/sub/contents/") && destination.endsWith("/watch")) {
      contentId = getContentId(destination);
    } else {
      return;
    }

    if (contentId != null) {
      User user = getUser(accessor, sessionId);
      UUID contentUUID = UUID.fromString(contentId);
      WatchingSession existingSession = watchingSessionRepository.findByUserIdAndContentId(user.getId(), UUID.fromString(contentId))
          .orElse(null);

      if (existingSession != null) {
        UUID watchingSessionId = existingSession.getId();
        accessor.getSessionAttributes().put("watchingSessionId", watchingSessionId);
        accessor.getSessionAttributes().put("watchingContentId", contentUUID);
        log.info("[WebsocketEventListener] 세션 정보 저장 완료: userId={}, watchingSessionId={}",
            user.getId(), existingSession.getId());
        String payloadDestination = String.format("/sub/contents/%s/watch", contentId);
        redisPublisher.convertAndSend(payloadDestination, watchingSessionChange);
      } else {
        // 예외 케이스 - HTTP 요청 없이 소켓만 연결된 경우
        log.warn("[WebsocketEventListener] DB에 시청 세션이 없습니다. Controller 로직이 먼저 실행되어야 합니다.");
      }
    }
  }
  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();

    UUID watchingSessionId = (UUID) accessor.getSessionAttributes().get("watchingSessionId");
    UUID contentId = (UUID) accessor.getSessionAttributes().get("watchingContentId");

    log.info("[WebsocketEventListener] SessionDisconnectEvent 시작 - sessionId: {}, watchingSessionId: {}, contentId: {}",
        sessionId, watchingSessionId, contentId);

    if (watchingSessionId == null || contentId == null) {
      log.warn("[WebsocketEventListener] SessionDisconnectEvent: watchingSessionId = {}, contentId = {}", watchingSessionId, contentId);
      return;
    }
    User user = getUser(accessor, sessionId);
    log.info("[WebsocketEventListener] SessionDisconnectEvent: 서비스에서 DB 삭제 시작: watchingSessionId={}", watchingSessionId);
    WatchingSessionChange watchingSessionChange = service.leaveSession(user, watchingSessionId, contentId);
    log.info("[WebsocketEventListener] SessionDisconnectEvent: 서비스에서 DB 삭제 성공: savedWatchingSessionId={}", watchingSessionId);
    long watcherCount = watchingSessionChange.watcherCount();

    String payloadDestination = String.format("/sub/contents/%s/watch", contentId);

    log.info("[WebsocketEventListener] handleSessionDisconnect 완료 - userId: {}, contentId: {}, watcherCount: {}",
        user.getId(), contentId, watcherCount);
//    messagingTemplate.convertAndSend(payloadDestination, watchingSessionChange);
    redisPublisher.convertAndSend(payloadDestination, watchingSessionChange);
  }

  // ==================== helper methods ====================
  private User getUser(StompHeaderAccessor accessor, String sessionId) {
    log.info("[WebsocketEventListener] getUser 시작 - sessionId: {}", sessionId);

    Authentication authentication = (Authentication) accessor.getUser();
    if (authentication == null) {
      log.error("[WebsocketEventListener] getUser: 세션 {}에 대해 사용자가 인증되지 않았습니다.", sessionId);
      throw new UserNotAuthenticatedException(WatchingSessionErrorCode.USER_NOT_AUTHENTICATED, Map.of("session", sessionId));
    }

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    UUID userId = userDetails.getUser().id();
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    log.info("[WebsocketEventListener] getUser 완료 - userId: {}", userId);
    return user;
  }

  private String getContentId(String destination) {
    log.info("[WebsocketEventListener] getContentId 시작 - destination: {}", destination);

    try {
        String[] parts = destination.split("/");
        String contentId = parts[3];
        log.info("[WebSocketEventListener] 컨텐트 아이디 파싱 완료: contentId={}", contentId);
        return contentId;
      } catch (Exception e) {
        log.error("[WebSocketEventListener] 컨텐츠 아이디 파싱 오류");
        throw new IllegalArgumentException("컨텐츠 아이디 파싱 오류", e);
      }
  }

}
