package com.codeit.mopl.domain.watchingsession.service;

import com.codeit.mopl.domain.watchingsession.dto.MessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/*
   Class that receives messages published in Redis Channel
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

  private final SimpMessagingTemplate template;
  private final ObjectMapper mapper;

  public void onMessage(String message) throws JsonProcessingException {
    MessagePayload payload = mapper.readValue(message, MessagePayload.class);
    String destination = payload.destination();
    log.info("[웹소켓 (Redis)] 메세지 수신, destination = {}", destination);
    template.convertAndSend(payload.destination(), payload.content());
  }

}