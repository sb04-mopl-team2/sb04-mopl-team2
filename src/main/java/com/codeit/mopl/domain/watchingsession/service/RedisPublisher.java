package com.codeit.mopl.domain.watchingsession.service;

import com.codeit.mopl.domain.watchingsession.dto.MessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

// 각각의 서버 -> Redis DB
// Topic으로 푸쉬
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ChannelTopic topic;

  // Object -> contentChatDto, WatchingSessionChange
  public void convertAndSend(String destination, Object object) {
    log.info("[웹소켓 (Redis)] Topic publish 시작, destination = {}, topic = {}",
        topic.getTopic(), destination);
    redisTemplate.convertAndSend(
        topic.getTopic(),
        new MessagePayload(destination, object)
    );
    log.info("[웹소켓 (Redis)] Topic publish 완료, destination = {}, topic = {}",
        topic.getTopic(), destination);
  }
}
