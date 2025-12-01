package com.codeit.mopl.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
  public ObjectMapper redisObjectMapper() {
    ObjectMapper om = new ObjectMapper();

    // LocalDateTime 같은 Java 8 time 지원
    om.registerModule(new JavaTimeModule());
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // 캐시에 넣었다 뺄 때 타입 정보 유지 (LinkedHashMap 방지)
    om.activateDefaultTyping(
        om.getPolymorphicTypeValidator(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );

    return om;
  }

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
