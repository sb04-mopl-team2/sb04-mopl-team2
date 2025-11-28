package com.codeit.mopl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public RedisCacheConfiguration redisCacheConfiguration(ObjectMapper redisObjectMapper) {
    GenericJackson2JsonRedisSerializer serializer =
        new GenericJackson2JsonRedisSerializer(redisObjectMapper);

    return RedisCacheConfiguration.defaultCacheConfig()
        .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(SerializationPair.fromSerializer(serializer))
        .entryTtl(Duration.ofMinutes(5));
  }
}
