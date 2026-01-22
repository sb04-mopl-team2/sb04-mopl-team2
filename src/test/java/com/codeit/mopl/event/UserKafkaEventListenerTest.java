package com.codeit.mopl.event;

import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.event.event.UserLogInOutEvent;
import com.codeit.mopl.event.event.UserRoleUpdateEvent;
import com.codeit.mopl.event.listener.KafkaEventListener;
import com.codeit.mopl.event.sender.KafkaEventSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserKafkaEventListenerTest {

    @Mock
    private KafkaEventSender sender;

    @InjectMocks
    private KafkaEventListener kafkaEventListener;

    @BeforeEach
    void setUp() {
        MDC.put("requestId", UUID.randomUUID().toString());
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
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
        verify(sender, times(1)).send(
                eq("mopl-user-role-update"),
                eq(userId.toString()),
                eq(event)
        );
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
        verify(sender, times(1)).send(
                eq("mopl-user-login-out"),
                eq(userId.toString()),
                eq(event)
        );
    }
}
