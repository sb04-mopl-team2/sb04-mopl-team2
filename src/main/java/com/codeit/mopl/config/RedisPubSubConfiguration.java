package com.codeit.mopl.config;

import com.codeit.mopl.domain.watchingsession.dto.MessagePayload;
import com.codeit.mopl.domain.watchingsession.service.RedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
    We register our class to which channels it will subscribe to messages
 */
@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfiguration {

  private final RedisConnectionFactory connectionFactory;
  private final RedisSubscriber redisSubscriber;

  // topic
  @Bean
  public ChannelTopic chatTopic() {
    return new ChannelTopic("websocket-events");
  }

  // Delegates the handling of messages to target listener methods
  // onMessage() is in RedisSubscriber service
  @Bean
  public MessageListenerAdapter listenerAdapter() {
    return new MessageListenerAdapter(redisSubscriber, "onMessage");
  }

  // Container that handles the low level details of
  // listening, converting and message dispatching
  @Bean
  public RedisMessageListenerContainer redisContainer() {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listenerAdapter(), chatTopic());
    return container;
  }

  @Bean
  public RedisTemplate<String, Object> websocketChatRedisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // 키를 위한 직렬화 설정 (StringRedisSerializer)
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    // 값을 위한 긱렬화 설정 (Jackson2JsonRedisSerializer)
    Jackson2JsonRedisSerializer<MessagePayload> serializer = new Jackson2JsonRedisSerializer<>(MessagePayload.class);
    template.setValueSerializer(serializer);
    template.setHashValueSerializer(serializer);

    return template;
  }
}
