package com.codeit.mopl.websocket;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import java.text.ParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/*
   WebSocket context에서 사용할 authentication object를 따로 만들고 저장해야함
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class AuthChannelInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      log.info("STOMP CONNECT 요청. 세션 ID: {}", accessor.getSessionId());

      String jwt = accessor.getFirstNativeHeader("Authorization");
      if (StringUtils.hasText(jwt) && jwt.startsWith("Bearer ")) {
        jwt = jwt.substring(7);
      }

      try {
        // 토큰 유효성 검사
        if (StringUtils.hasText(jwt)) {
          String userEmail = jwtTokenProvider.getEmail(jwt);
          UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

          UsernamePasswordAuthenticationToken authenticationToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails,
                  null, // 비번은 필요 없음
                  userDetails.getAuthorities()
              );
          // STOMP 세션에 인증 정보 설정
          accessor.setUser(authenticationToken);
          log.info("STOMP 인증 성공. 사용자: {}, 세션: {}", userEmail, accessor.getSessionId());        }
      } catch (AuthenticationException | JwtException e) {
        log.warn("STOMP CONNECT: Authorization 헤더에 JWT 토큰이 없습니다.");
        throw new AuthenticationException("JWT 토큰이 필요합니다.", e) {}; // 커스텀 exception
      } catch (Exception e) {
        log.error("STOMP 인증 처리 중 알 수 없는 예외 발생", e);
        throw new RuntimeException("인증 처리 중 오류가 발생했습니다.", e);
      }
    }

    if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
      log.info("STOMP DISCONNECT. 세션 ID: {}", accessor.getSessionId());
    }
    return message;
  }
}
