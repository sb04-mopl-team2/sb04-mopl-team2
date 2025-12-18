package com.codeit.mopl.config;

import com.codeit.mopl.websocket.AuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final AuthChannelInterceptor authChannelInterceptor;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws") // 최초 연결
        .setAllowedOriginPatterns("*") // CORS 허용
        .withSockJS(); // SockJS fallback
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/pub"); // client -> server
    registry.enableSimpleBroker("/sub"); // server -> client
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registry) {
    registry.interceptors(authChannelInterceptor);
  }
}
