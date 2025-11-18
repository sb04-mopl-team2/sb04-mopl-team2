package com.codeit.mopl.websocket;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.security.jwt.JwtTokenProvider;
import java.text.ParseException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class AuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthChannelInterceptor authChannelInterceptor;

    private MessageChannel mockChannel;
    private StompHeaderAccessor accessor;

    @BeforeEach
    void setUp() {
        mockChannel = mock(MessageChannel.class);
        accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 WebSocket 연결에 성공한다")
    void preSend_ValidToken() throws ParseException {
        // given
        String token = "valid-jwt-token";
        String userEmail = "test@example.com";
        
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

        UserDetails userDetails = User.builder()
            .username(userEmail)
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        when(jwtTokenProvider.getEmail(token)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        // when
        Message<?> result = authChannelInterceptor.preSend(message, mockChannel);

        // then
        verify(jwtTokenProvider).getEmail(token);
        verify(userDetailsService).loadUserByUsername(userEmail);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 예외가 발생한다")
    void preSend_MissingAuthHeader() {
        // given
        Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

        // when & then
        assertThatThrownBy(() -> authChannelInterceptor.preSend(message, mockChannel))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("JWT 토큰이 필요합니다");
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 토큰은 예외가 발생한다")
    void preSend_InvalidTokenFormat() {
        // given
        accessor.addNativeHeader("Authorization", "invalid-token");
        Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

        // when & then
        assertThatThrownBy(() -> authChannelInterceptor.preSend(message, mockChannel))
            .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰은 예외가 발생한다")
    void preSend_InvalidToken() throws ParseException {
        // given
        String invalidToken = "invalid-token";
        accessor.addNativeHeader("Authorization", "Bearer " + invalidToken);
        Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

        when(jwtTokenProvider.getEmail(invalidToken)).thenThrow(new ParseException("Invalid token", 0));

        // when & then
        assertThatThrownBy(() -> authChannelInterceptor.preSend(message, mockChannel))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("인증 처리 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("DISCONNECT 명령은 정상 처리된다")
    void preSend_DisconnectCommand() {
        // given
        StompHeaderAccessor disconnectAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Message<?> message = MessageBuilder.createMessage("", disconnectAccessor.getMessageHeaders());

        // when
        Message<?> result = authChannelInterceptor.preSend(message, mockChannel);

        // then - should not throw exception
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("CONNECT가 아닌 명령은 인증 검사를 하지 않는다")
    void preSend_NonConnectCommand() {
        // given
        StompHeaderAccessor sendAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage("", sendAccessor.getMessageHeaders());

        // when
        Message<?> result = authChannelInterceptor.preSend(message, mockChannel);

        // then - should not throw exception
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("빈 Authorization 헤더는 예외가 발생한다")
    void preSend_EmptyAuthHeader() {
        // given
        accessor.addNativeHeader("Authorization", "");
        Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

        // when & then
        assertThatThrownBy(() -> authChannelInterceptor.preSend(message, mockChannel))
            .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("Bearer만 있고 토큰이 없으면 예외가 발생한다")
    void preSend_BearerOnly() {
        // given
        accessor.addNativeHeader("Authorization", "Bearer ");
        Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

        // when & then
        assertThatThrownBy(() -> authChannelInterceptor.preSend(message, mockChannel))
            .isInstanceOf(AuthenticationException.class);
    }
}