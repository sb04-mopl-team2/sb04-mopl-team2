package com.codeit.mopl.domain.websocket.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.message.conversation.repository.ConversationSubscriptionRegistry;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.domain.watchingsession.service.RedisPublisher;
import com.codeit.mopl.domain.watchingsession.service.WatchingSessionService;
import com.codeit.mopl.event.watchingsession.WebSocketEventListener;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.util.WithCustomMockUser;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
public class WebSocketEventListenerTest {

  @Mock
  private WatchingSessionService service;

  @Mock
  private RedisPublisher redisPublisher;

  @Mock
  private ConversationSubscriptionRegistry conversationSubscriptionRegistry;

  @InjectMocks
  private WebSocketEventListener eventListener;

  private UUID contentId;
  private UUID userId;
  private UUID watchingSessionId;
  private UUID sessionId;
  private String validDestination;

  @BeforeEach
  public void init() {
    contentId = UUID.randomUUID();
    userId = UUID.randomUUID();
    watchingSessionId = UUID.randomUUID();
    sessionId =  UUID.randomUUID();
    validDestination = "/sub/contents/" + contentId + "/watch";
  }

  @Test
  @WithCustomMockUser
  @DisplayName("SessionSubscribeEvent 성공")
  public void handleSessionSubscribeSuccess() {
    // given
    StompHeaderAccessor accessor = getValidStompHeaderAccessor();
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, accessor.getUser());

    WatchingSessionChange watchingSessionChange = mock(WatchingSessionChange.class);
    WatchingSessionDto watchingSessionDto = mock(WatchingSessionDto.class);
    when(service.joinSession(userId, contentId)).thenReturn(watchingSessionChange);
    when(watchingSessionChange.watchingSession()).thenReturn(watchingSessionDto);
    when(watchingSessionDto.id()).thenReturn(watchingSessionId);

    // when
    eventListener.handleSessionSubscribe(event);

    // then
    verify(service).joinSession(userId, contentId);
    verify(redisPublisher).convertAndSend(validDestination, watchingSessionChange);
    assertThat(accessor.getSessionAttributes().get("watchingSessionId")).isEqualTo(watchingSessionId);
    assertThat(accessor.getSessionAttributes().get("watchingContentId")).isEqualTo(contentId);
  }

  @Test
  @DisplayName("SessionSubscribeEvent - 잘못된 destination이면 무시됨")
  public void handleSessionSubscribeInvalidDestinationIgnored() {
    // given
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/invalid_destination");
    accessor.setSessionAttributes(new HashMap<>());
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, null);

    // when
    eventListener.handleSessionSubscribe(event);

    // then
    verify(service, never()).joinSession(any(UUID.class), any(UUID.class));
    verify(redisPublisher, never()).convertAndSend(any(String.class), any(WatchingSessionChange.class));
  }

  @Test
  @DisplayName("SessionSubscribeEvent - destination이 null이면 무시됨")
  public void handleSessionSubscribeNullDestinationIgnored() {
    // given
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionAttributes(new HashMap<>());
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, null);

    // when
    eventListener.handleSessionSubscribe(event);

    // then
    verify(service, never()).joinSession(any(UUID.class), any(UUID.class));
    verify(redisPublisher, never()).convertAndSend(any(String.class), any(WatchingSessionChange.class));
  }

  @Test
  @DisplayName("SessionUnsubscribeEvent - 다른 열린 세션 없으면 session 제거")
  public void handleSessionUnSubscribeNoSession() {
    // given
    StompHeaderAccessor accessor = getValidStompHeaderAccessor();
    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    sessionAttributes.put("watchingSessionId", watchingSessionId);
    sessionAttributes.put("watchingContentId", contentId);
    accessor.setSessionAttributes(sessionAttributes);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message, accessor.getUser());

    WatchingSessionChange mockWatchingSessionChange = mock(WatchingSessionChange.class);
    when(service.leaveSession(userId, watchingSessionId, contentId)).thenReturn(mockWatchingSessionChange);
    when(mockWatchingSessionChange.watcherCount()).thenReturn(0L);

    // when
    eventListener.handleSessionUnSubscribe(event);

    // then
    assertThat(sessionAttributes.get("watchingSessionId")).isNull();
    assertThat(sessionAttributes.get("watchingContentId")).isNull();
    verify(service).leaveSession(userId, watchingSessionId, contentId);
    verify(redisPublisher).convertAndSend(String.format("/sub/contents/%s/watch", contentId), mockWatchingSessionChange);
  }

  @Test
  @DisplayName("SessionUnsubscribeEvent - watchingSessionId가 없으면 리턴")
  public void handleSessionUnsubscribeNoWatchingSessionId() {
    // given
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionAttributes(new HashMap<>());
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message, null);

    // when
    eventListener.handleSessionUnSubscribe(event);

    // then
    verify(service, never()).leaveSession(any(UUID.class), any(UUID.class), any(UUID.class));
    verify(redisPublisher, never()).convertAndSend(any(String.class), any(WatchingSessionChange.class));
  }


  @Test
  @DisplayName("SessionDisconnectEvent - 다른 열린 세션 있으면 리턴")
  public void handleSessionDisconnectSession() {
    // given
    SessionDisconnectEvent event = getSessionDisconnectEvent();
    WatchingSessionChange mockWatchingSessionChange = mock(WatchingSessionChange.class);
    when(service.leaveSession(userId, watchingSessionId, contentId)).thenReturn(mockWatchingSessionChange);
    when(mockWatchingSessionChange.watcherCount()).thenReturn(0L);

    // when
    eventListener.handleSessionDisconnect(event);

    // then
    verify(service).leaveSession(userId, watchingSessionId, contentId);
    verify(redisPublisher).convertAndSend(String.format("/sub/contents/%s/watch", contentId), mockWatchingSessionChange);
  }

  @Test
  @DisplayName("DM 구독 시 Redis에 join 호출되고 세션 속성에 저장된다")
  public void handleSessionSubscribe_dm_joinsRedisAndStoresInSession() {
    // given
    UUID conversationId = UUID.randomUUID();
    String dmDestination = "/sub/conversations/" + conversationId + "/direct-messages";
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionId(String.valueOf(sessionId));
    accessor.setDestination(dmDestination);
    accessor.setSubscriptionId("sub-1");
    accessor.setSessionAttributes(new HashMap<>());
    accessor.setUser(mockAuthentication(userId));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, accessor.getUser());

    // when
    eventListener.handleSessionSubscribe(event);

    // then
    verify(conversationSubscriptionRegistry).join(userId, conversationId);
    verify(service, never()).joinSession(any(), any());
    @SuppressWarnings("unchecked")
    Map<String, UUID> dmSubscriptions = (Map<String, UUID>) accessor.getSessionAttributes().get("dmSubscriptions");
    assertThat(dmSubscriptions).containsEntry("sub-1", conversationId);
  }

  @Test
  @DisplayName("DM 구독 해제 시 Redis에 leave 호출되고 watching session 로직은 실행되지 않는다")
  public void handleSessionUnSubscribe_dm_leavesRedis() {
    // given
    UUID conversationId = UUID.randomUUID();
    Map<String, UUID> dmSubscriptions = new HashMap<>();
    dmSubscriptions.put("sub-1", conversationId);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
    accessor.setSessionId(String.valueOf(sessionId));
    accessor.setSubscriptionId("sub-1");
    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put("dmSubscriptions", dmSubscriptions);
    accessor.setSessionAttributes(sessionAttributes);
    accessor.setUser(mockAuthentication(userId));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message, accessor.getUser());

    // when
    eventListener.handleSessionUnSubscribe(event);

    // then
    verify(conversationSubscriptionRegistry).leave(userId, conversationId);
    verify(service, never()).leaveSession(any(), any(), any());
  }

  @Test
  @DisplayName("Disconnect 시 열려있던 DM 구독 전부 Redis leave 처리된다")
  public void handleSessionDisconnect_cleansUpDmSubscriptions() {
    // given
    UUID conversationId1 = UUID.randomUUID();
    UUID conversationId2 = UUID.randomUUID();
    Map<String, UUID> dmSubscriptions = new HashMap<>();
    dmSubscriptions.put("sub-1", conversationId1);
    dmSubscriptions.put("sub-2", conversationId2);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    accessor.setSessionId(String.valueOf(sessionId));
    accessor.setUser(mockAuthentication(userId));
    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put("dmSubscriptions", dmSubscriptions);
    accessor.setSessionAttributes(sessionAttributes);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionDisconnectEvent event = new SessionDisconnectEvent(
        this, message, String.valueOf(sessionId), CloseStatus.NORMAL
    );

    // when
    eventListener.handleSessionDisconnect(event);

    // then
    verify(conversationSubscriptionRegistry).leave(userId, conversationId1);
    verify(conversationSubscriptionRegistry).leave(userId, conversationId2);
    verify(service, never()).leaveSession(any(), any(), any());
  }

  // ========================= 헬퍼 메서드 =========================
  private SessionDisconnectEvent getSessionDisconnectEvent() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    accessor.setSessionId(String.valueOf(sessionId));
    accessor.setUser(mockAuthentication(userId));
    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put("watchingSessionId", watchingSessionId);
    sessionAttributes.put("watchingContentId", contentId);
    accessor.setSessionAttributes(sessionAttributes);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionDisconnectEvent event = new SessionDisconnectEvent(
        this, message, String.valueOf(sessionId), CloseStatus.NORMAL
    );
    return event;
  }

  private StompHeaderAccessor getValidStompHeaderAccessor() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionId(String.valueOf(sessionId));
    accessor.setDestination(validDestination);
    accessor.setSessionAttributes(new HashMap<>());
    accessor.setUser(mockAuthentication(userId));
    return accessor;
  }

  private Authentication mockAuthentication(UUID userId) {
    Authentication auth = mock(Authentication.class);
    CustomUserDetails userDetails = mock(CustomUserDetails.class);
    UserDto dto = mock(UserDto.class);
    when(dto.id()).thenReturn(userId);
    when(userDetails.getUser()).thenReturn(dto);
    when(auth.getPrincipal()).thenReturn(userDetails);
    return auth;
  }
}
