package com.codeit.mopl.event.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class KafkaAckManagerTest {

    @Autowired
    KafkaAckManager ackManager;

    @Test
    @DisplayName("트랜잭션 커밋 후 ack를 호출한다.")
    @Transactional
    void ackAfterCommit() {
        // given
        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        ackManager.ackAfterCommit(ack);

        // verify
        // 트랜잭션 커밋 전이므로 호출되지 않음
        verify(ack, never()).acknowledge();
    }
}