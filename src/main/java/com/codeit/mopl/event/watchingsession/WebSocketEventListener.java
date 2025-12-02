package com.codeit.mopl.event.watchingsession;

import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.domain.watchingsession.service.WatchingSessionService;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.exception.watchingsession.UserNotAuthenticatedException;
import com.codeit.mopl.exception.watchingsession.WatchingSessionErrorCode;
import com.codeit.mopl.security.CustomUserDetails;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final WatchingSessionService service;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final SimpUserRegistry userRegistry;

  /**
     콘텐츠 시청 세션: 누가 시청 세션에 들어오고 나가는지 (참가자 목록) 업데이트를 받기 위해
     - 엔드포인트: SUBSCRIBE /sub/contents/{contentId}/watch
     - 페이로드: WatchingSessionChange
     - 참고 - https://hong-good.tistory.com/7
   **/

  /*
    실시간 채팅 UI에 유저 참여 (추가) 바로 업데이트
   */
  @EventListener
  public void handleSessionSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    String destination = accessor.getDestination();

    log.info("[WebsocketEventListener] handleSessionSubscribe 시작 - sessionId: {}, destination: {}", sessionId, destination);

    if (destination == null) return;

    if (destination.startsWith("/sub/contents/") && destination.endsWith("/watch")) {
      String contentId = getContentId(destination);
      UUID userId = getUserId(accessor, sessionId);
      UUID contentUUID = UUID.fromString(contentId);

      WatchingSessionChange watchingSessionChange = service.joinSession(userId, contentUUID);

      accessor.getSessionAttributes().put("watchingSessionId", watchingSessionChange.watchingSession().id());
      accessor.getSessionAttributes().put("watchingContentId", contentUUID);
      log.info("[WebsocketEventListener] handleSessionSubscribe accessor에 값 넣어줌 - watchingSessionId: {}, contentUUID: {}",
          watchingSessionChange.watchingSession().id(), contentUUID);

      String payloadDestination = String.format("/sub/contents/%s/watch", contentId);
      messagingTemplate.convertAndSend(payloadDestination, watchingSessionChange);

      } else {
      // 예외 케이스 - HTTP 요청 없이 소켓만 연결된 경우
      log.warn("[WebsocketEventListener] DB에 시청 세션이 없습니다. Controller 로직이 먼저 실행되어야 합니다.");
    }
  }

  /*
    실시간 채팅 UI에 유저 퇴장 바로 업데이트
    - 페이지 이동(뒤로 가기) 시 퇴장 처리
   */
  @EventListener
  public void handleSessionUnSubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    UUID watchingSessionId = (UUID) accessor.getSessionAttributes().get("watchingSessionId");
    UUID contentId = (UUID) accessor.getSessionAttributes().get("watchingContentId");
    log.info("[WebsocketEventListener] SessionUnsubscribeEvent 시작 - sessionId: {}, watchingSessionId: {}, contentId: {}",
        sessionId, watchingSessionId, contentId);

    if (watchingSessionId == null || contentId == null) {
      log.warn("[WebsocketEventListener] SessionUnsubscribeEvent: watchingSessionId = {}, contentId = {}", watchingSessionId, contentId);
      return;
    }

    UUID userId = getUserId(accessor, sessionId);

    // check for other sessions
    if (userWatchingOnOtherSession(userId, contentId)) return;

    accessor.getSessionAttributes().remove("watchingSessionId");
    accessor.getSessionAttributes().remove("watchingContentId");
    log.info("[WebsocketEventListener] SessionUnsubscribeEvent 완료 - 속성 제거됨");

    processLeave(watchingSessionId, userId, contentId);
    log.info("[WebsocketEventListener] SessionUnsubscribeEvent 완료 - sessionId: {}, watchingSessionId: {}, contentId: {}",
        sessionId, watchingSessionId, contentId);
  }

  /*
    웹소켓 연결이 끊일 때 대비하는 용도
    - 크롬 닫힘, 탭 닫기(강제 종료)
   */
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

    UUID userId = getUserId(accessor, sessionId);
    if (userWatchingOnOtherSession(userId, contentId)) {
      return;
    }

    processLeave(watchingSessionId, userId, contentId);
    log.info("[WebsocketEventListener] SessionDisconnectEvent 완료 - sessionId: {}, watchingSessionId: {}, contentId: {}",
        sessionId, watchingSessionId, contentId);
  }

  // ================================== helper methods ==================================

  private boolean userWatchingOnOtherSession(UUID userId, UUID contentId) {
    User foundUser = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(
            UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    String username = foundUser.getEmail();
    SimpUser simpUser = userRegistry.getUser(username);
    if (simpUser == null) return false;
    Set<SimpSession> userSessions = simpUser.getSessions();
    if (userSessions.isEmpty()) return false;

    for (SimpSession s : userSessions) {
      for (SimpSubscription sub : s.getSubscriptions()) {
        String payloadDestination = String.format("/sub/contents/%s/watch", contentId);
        if (sub.getDestination().equals(payloadDestination)) return true;
      }
    }
    return false;
  }

  private void processLeave(UUID watchingSessionId, UUID userId, UUID contentId) {
    log.info("[WebsocketEventListener] processLeave 시작: watchingSessionId={}",
        watchingSessionId);
    WatchingSessionChange watchingSessionChange = service.leaveSession(userId, watchingSessionId,
        contentId);
    log.info("[WebsocketEventListener] processLeave - 서비스에서 DB 삭제 성공: savedWatchingSessionId={}",
        watchingSessionId);
    long watcherCount = watchingSessionChange.watcherCount();

    String payloadDestination = String.format("/sub/contents/%s/watch", contentId);
    messagingTemplate.convertAndSend(payloadDestination, watchingSessionChange);

    log.info("[WebsocketEventListener] processLeave 완료 - userId: {}, contentId: {}, watcherCount: {}",
        userId, contentId, watcherCount);
  }

  private UUID getUserId(StompHeaderAccessor accessor, String sessionId) {
    log.info("[WebsocketEventListener] getUser 시작 - sessionId: {}", sessionId);

    Authentication authentication = (Authentication) accessor.getUser();
    if (authentication == null) {
      log.error("[WebsocketEventListener] getUser - 세션 {}에 대해 사용자가 인증되지 않았습니다.", sessionId);
      throw new UserNotAuthenticatedException(WatchingSessionErrorCode.USER_NOT_AUTHENTICATED, Map.of("session", sessionId));
    }

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    UUID userId = userDetails.getUser().id();

    log.info("[WebsocketEventListener] getUser 완료 - userId: {}", userId);
    return userId;
  }

  private String getContentId(String destination) {
    log.info("[WebsocketEventListener] getContentId 시작 - destination: {}", destination);

    try {
        String[] parts = destination.split("/");
        String contentId = parts[3];
        log.info("[WebSocketEventListener] getContentId - 컨텐트 아이디 파싱 완료: contentId={}", contentId);
        return contentId;
      } catch (Exception e) {
        log.error("[WebSocketEventListener] 컨텐츠 아이디 파싱 오류");
        throw new IllegalArgumentException("컨텐츠 아이디 파싱 오류", e);
      }
  }

}
