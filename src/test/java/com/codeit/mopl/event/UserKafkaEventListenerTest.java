package com.codeit.mopl.event;

import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.event.event.UserLogInOutEvent;
import com.codeit.mopl.event.event.UserRoleUpdateEvent;
import com.codeit.mopl.event.listener.KafkaEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserKafkaEventListenerTest {
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaEventListener kafkaEventListener;

    @BeforeEach
    void setUp() {
        MDC.put("requestId", UUID.randomUUID().toString());
        CompletableFuture<SendResult<String, String>> future =
                (CompletableFuture<SendResult<String, String>>) mock(CompletableFuture.class);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
    }
    @Test
    @DisplayName("UserRoleUpdateEventListener")
    void userRoleUpdateEventListener() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserRoleUpdateEvent event = new UserRoleUpdateEvent(
                eventId,
                userId,
                Role.USER,
                Role.ADMIN
        );

        // when
        kafkaEventListener.on(event);

        // then
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    @DisplayName("UserLogInOutEventListener")
    void userLogInOutEventListener() {
        UUID userId = UUID.randomUUID();
        UserLogInOutEvent event = new UserLogInOutEvent(
                userId,
                true
        );

        // when
        kafkaEventListener.on(event);

        // then
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }
}
