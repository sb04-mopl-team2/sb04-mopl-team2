package com.codeit.mopl.event.watchingsession;

import com.codeit.mopl.domain.content.dto.response.ContentSummary;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.enums.ChangeType;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.mopl.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final WatchingSessionRepository watchingSessionRepository;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;
  private final SimpMessagingTemplate messagingTemplate;
  
  /*
     콘텐츠 시청 세션: 누가 시청 세션에 들어오고 나가는지 (참가자 목록) 업데이트를 받기 위해
     - 엔드포인트: SUBSCRIBE /sub/contents/{contentId}/watch
     - 페이로드: WatchingSessionChange
   */
  @EventListener
  @Transactional
  public void handleSessionSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId(); // websocket connection identifier
    String destination = accessor.getDestination();

    if (destination == null) {
      log.error("[WebsocketEventListener] SessionSubscribeEvent: destination이 비었습니다!");
      return;
    }

    // extract contentId
    String contentId = null;
    if (destination.startsWith("/sub/contents/") && destination.endsWith("watch")) {
      contentId = getContentId(destination);
    }

    if (contentId != null) {
      // get user and content info
      User user = getUser(accessor, sessionId);
      Content content = contentRepository.findById(UUID.fromString(contentId))
          .orElseThrow(() -> new RuntimeException("user not found"));

      // disconnect any other watching session for user (1 session per user)
      watchingSessionRepository.deleteByUserId(user.getId());

      // create new WatchingSession and save to repository
      WatchingSession watchingSession = new WatchingSession();
      watchingSession.setUser(user);
      watchingSession.setContent(content);
      WatchingSession savedWatchingSession = watchingSessionRepository.save(watchingSession);

      // add to websocket session attributes
      accessor.getSessionAttributes().put("watchingSessionId", savedWatchingSession.getId());
      accessor.getSessionAttributes().put("watchingContentId", contentId);

      // create payload
      Long watcherCount = watchingSessionRepository.countByContentId(UUID.fromString(contentId));
      WatchingSessionChange watchingSessionChange = getWatchingSessionChange(
          savedWatchingSession, user, ChangeType.JOIN, watcherCount);

      // server -> client
      String payloadDestination = String.format("/sub/contents/%s/watch", contentId);
      messagingTemplate.convertAndSend(payloadDestination, watchingSessionChange);
    }
  }

  @EventListener
  @Transactional
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();

    UUID watchingSessionId = (UUID) accessor.getSessionAttributes().get("watchingSessionId");
    String contentId = (String) accessor.getSessionAttributes().get("watchingContentId");

    if (watchingSessionId == null || contentId == null) {
      log.warn("[WebsocketEventListener] SessionDisconnectEvent: sessionId = {}, contentId = {}", sessionId, contentId);
      return;
    }

    // get user, watchingsession, watcherCount
    User user = getUser(accessor, sessionId);
    WatchingSession watchingSession = watchingSessionRepository.findById(watchingSessionId)
        .orElseThrow(() -> new RuntimeException("watchingsession for user does not exist"));
    watchingSessionRepository.deleteById(watchingSessionId);
    long watcherCount = watchingSessionRepository.countByContentId(UUID.fromString(contentId));

    // payload
    WatchingSessionChange watchingSessionChange = getWatchingSessionChange(
        watchingSession, user, ChangeType.LEAVE, watcherCount);

    String payloadDestination = String.format("/sub/contents/%s/watch", contentId);
    messagingTemplate.convertAndSend(payloadDestination, watchingSessionChange);
  }

  // ==================== helper methods ====================
  private User getUser(StompHeaderAccessor accessor, String sessionId) {
    Authentication authentication = (Authentication) accessor.getUser();
    if (authentication == null) {
      log.error("[WebsocketEventListener] getUser: 세션 {}에 대해 사용자가 인증되지 않았습니다.", sessionId);
      // throw custom exception
      throw new RuntimeException("User is not authenticated");
    }

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    return userRepository.findById(userDetails.getUser().id())
        .orElseThrow(() -> new RuntimeException("user not found"));
  }

  private String getContentId(String destination) {
      try {
        String[] parts = destination.split("/");
        String contentId = parts[3];
        log.info("WebSocketEventListener: contentId={}", contentId);
        return contentId;
      } catch (Exception e) {
        log.error("WebSocketEventListener: error while parsing contentId from destination!");
        throw new RuntimeException("error while parsing contentId from destination");
      }
  }

  private static WatchingSessionChange getWatchingSessionChange(
      WatchingSession savedWatchingSession, User user, ChangeType changeType, Long watcherCount) {
    Content content = savedWatchingSession.getContent();

    return new WatchingSessionChange(
        changeType,
        new WatchingSessionDto(
            savedWatchingSession.getId(),
            savedWatchingSession.getCreatedAt(),
            new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getProfileImageUrl()
            ),
            new ContentSummary(
                content.getId(),
                content.getContentType().getType(),
                content.getTitle(),
                content.getDescription(),
                content.getThumbnailUrl(),
                content.getTags(),
                content.getAverageRating(),
                content.getReviewCount()
            )
        ),
        watcherCount
    );
  }
}
