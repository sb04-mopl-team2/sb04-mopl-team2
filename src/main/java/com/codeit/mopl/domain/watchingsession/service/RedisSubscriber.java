package com.codeit.mopl.domain.watchingsession.service;

import com.codeit.mopl.domain.watchingsession.dto.MessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/*
   Redis 채널에 publish된 메세지를 받는 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

  private final SimpMessagingTemplate template;
  private final ObjectMapper mapper;

  public void onMessage(String message) throws JsonProcessingException {
    log.info("[웹소켓 (Redis)] onMessage - 받은 메시지 (raw): {}", message);

    try {
      MessagePayload payload = mapper.readValue(message, MessagePayload.class);
      String destination = payload.destination();
      log.info("[웹소켓 (Redis)] onMessage - 메세지 수신, destination = {}", destination);
      template.convertAndSend(payload.destination(), payload.content());
    } catch (JsonProcessingException e) {
      log.error("[웹소켓 (Redis)] onMessage - 메시지 역직렬화 실패: {}", message, e);
    }
  }

}