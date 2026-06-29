package com.codeit.mopl.domain.message.conversation.repository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationSubscriptionRegistry {

  private static final String KEY_PREFIX = "dm:active:";

  private final StringRedisTemplate stringRedisTemplate;

  public void join(UUID userId, UUID conversationId) {
    stringRedisTemplate.opsForSet().add(key(userId), conversationId.toString());
  }

  public void leave(UUID userId, UUID conversationId) {
    stringRedisTemplate.opsForSet().remove(key(userId), conversationId.toString());
  }

  public boolean isActive(UUID userId, UUID conversationId) {
    Boolean result = stringRedisTemplate.opsForSet().isMember(key(userId), conversationId.toString());
    return Boolean.TRUE.equals(result);
  }

  private String key(UUID userId) {
    return KEY_PREFIX + userId;
  }
}