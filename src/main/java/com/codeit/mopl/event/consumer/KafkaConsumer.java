package com.codeit.mopl.event.consumer;


import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  @KafkaListener(topics = "mopl-notification-create", groupId = "mopl-notification", concurrency = "3")
  public void onNotificationCreated(String kafkaEventJson, Acknowledgment ack) throws Exception {
    NotificationCreateEvent event = objectMapper.readValue(kafkaEventJson, NotificationCreateEvent.class);
    NotificationDto notificationDto = event.notificationDto();
    notificationService.sendNotification(notificationDto);
    ack.acknowledge();
  }
}
