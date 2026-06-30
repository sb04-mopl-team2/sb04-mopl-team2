package com.codeit.mopl.domain.message.e2e.config;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * publish와 DM save 로직 검증을 위한 테스트용 WebSocket 설정
 * - 메시지 브로커로의 publish 동작 검증
 * - 실제 구독/수신은 검증하지 않음
 */
@TestConfiguration
@Profile("test")
@EnableWebSocketMessageBroker
public class TestWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserRepository userRepository;
    private static final Map<String, Authentication> sessionAuthMap = new ConcurrentHashMap<>();

    public TestWebSocketConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 브로커 설정 (publish 동작 검증을 위해 필요)
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                        String testUser = accessor.getFirstNativeHeader("X-TEST-USER");
                        User user = userRepository.findByEmail(
                                "sender".equals(testUser)
                                ? "test@example.com"
                                        :"receiver@example.com"
                        ).orElseThrow();

                        UserDto userDto = new UserDto(
                                user.getId(),
                                user.getCreatedAt(),
                                user.getEmail(),
                                user.getName(),
                                user.getProfileImageUrl(),
                                user.getRole(),
                                user.isLocked()
                        );

                        CustomUserDetails testUserDetails =
                                new CustomUserDetails(userDto, "test");

                        Authentication auth =
                                new UsernamePasswordAuthenticationToken(
                                        testUserDetails, "test", testUserDetails.getAuthorities());
                        accessor.setUser(auth);
                }
                return message;
            }
        });
    }
}
