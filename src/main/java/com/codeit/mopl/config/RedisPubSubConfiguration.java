package com.codeit.mopl.config;

import com.codeit.mopl.domain.watchingsession.dto.MessagePayload;
import com.codeit.mopl.domain.watchingsession.service.RedisSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfiguration {

  private final RedisConnectionFactory connectionFactory;
  private final RedisSubscriber redisSubscriber;

  // 토픽
  @Bean
  public ChannelTopic chatTopic() {
    return new ChannelTopic("websocket-events");
  }

  //  RedisSubscriber의 onMessage() -> 메세지 수신
  @Bean
  public MessageListenerAdapter listenerAdapter() {
    return new MessageListenerAdapter(redisSubscriber, "onMessage");
  }

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

    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Jackson2JsonRedisSerializer<MessagePayload> serializer =
        new Jackson2JsonRedisSerializer<>(objectMapper, MessagePayload.class);

    template.setValueSerializer(serializer);
    template.setHashValueSerializer(serializer);

    return template;
  }
}
