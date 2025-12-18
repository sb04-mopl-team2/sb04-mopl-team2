package com.codeit.mopl.config;

import java.time.Duration;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@Slf4j
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(ObjectMapper objectMapper) {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        RecordSupportingTypeResolver typeResolver = new RecordSupportingTypeResolver(ObjectMapper.DefaultTyping.NON_FINAL, redisObjectMapper.getPolymorphicTypeValidator());
        StdTypeResolverBuilder initializedResolver = typeResolver.init(JsonTypeInfo.Id.CLASS, null);
        initializedResolver = initializedResolver.inclusion(JsonTypeInfo.As.PROPERTY);
        redisObjectMapper.setDefaultTyping(initializedResolver);

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(redisObjectMapper)
                        )
                )
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                if (isRedisDown(e)) {
                    log.error("[Cache] Redis가 동작 중이 아님, cache={}, key={}",
                            cache != null ? cache.getName() : "null", key, e);
                    return;
                }
                throw e;
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                if (isRedisDown(e)) {
                    log.error("[Cache] Redis가 동작 중이 아님, cache={}, key={}",
                            cache != null ? cache.getName() : "null", key, e);
                    return;
                }
                throw e;
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                if (isRedisDown(e)) {
                    log.error("[Cache] Redis가 동작 중이 아님, cache={}, key={}",
                            cache != null ? cache.getName() : "null", key, e);
                    return;
                }
                throw e;
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                if (isRedisDown(e)) {
                    log.error("[Cache] Redis가 동작 중이 아님, cache={}",
                            cache != null ? cache.getName() : "null", e);
                    return;
                }
                throw e;
            }

            private boolean isRedisDown(Throwable e) {
                while (e != null) {
                    if (e instanceof RedisConnectionFailureException ||
                            e instanceof RedisConnectionException) {
                        return true;
                    }
                    e = e.getCause();
                }
                return false;
            }
        };
    }

    public class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

        public RecordSupportingTypeResolver(ObjectMapper.DefaultTyping t, PolymorphicTypeValidator ptv) {
            super(t, ptv);
        }

        @Override
        public boolean useForType(JavaType t) {
            boolean isRecord = t.getRawClass().isRecord();
            boolean superResult = super.useForType(t);

            if (isRecord) {
                return true;
            }
            return superResult;
        }
    }
}
