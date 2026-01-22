package com.codeit.mopl.event.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventSenderTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaEventSender kafkaEventSender;

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    @DisplayName("이벤트 전송 성공 시 헤더와 함께 카프카 메시지를 전송한다.")
    void send_success_shouldSendKafkaMessageWithHeaders() throws Exception {
        // given
        TestEvent payload = new TestEvent("test");

        String json = "{\"payload\":\"kim\"}";
        when(objectMapper.writeValueAsString(payload)).thenReturn(json);

        // KafkaTemplate.send(...) 이 CompletableFuture 를 반환하도록 stubbing
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // MDC 세팅
        MDC.put("requestId", "trace-123");

        // when
        CompletableFuture<SendResult<String, String>> result =
                kafkaEventSender.send("test-topic", "test-key", payload);

        // then
        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate, times(1)).send(recordCaptor.capture());
        ProducerRecord<String, String> record = recordCaptor.getValue();

        assertThat(result).isCompleted();

        assertThat(record.topic()).isEqualTo("test-topic");
        assertThat(record.key()).isEqualTo("test-key");
        assertThat(record.value()).isEqualTo(json);

        assertThat(headerValue(record, "x-trace-id")).isEqualTo("trace-123");
        assertThat(headerValue(record, "x-event-type")).isEqualTo("TestEvent");
    }

    @Test
    @DisplayName("전송 실패 테스트")
    void send_whenKafkaSendFails_shouldNotThrowNpe() throws Exception {
        // given
        TestEvent event = new TestEvent("fail");
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("kafka down"));

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(failedFuture);

        // when
        CompletableFuture<SendResult<String, String>> result =
                kafkaEventSender.send("topic", "key", event);

        // then
        assertThat(result).isCompletedExceptionally();
    }

    @Test
    @DisplayName("JSON 직렬화 실패 시 실패 상태의 Future를 반환한다.")
    void send_whenSerializationFails_shouldReturnFailedFuture() throws Exception {
        // given
        TestEvent payload = new TestEvent("fail");

        when(objectMapper.writeValueAsString(payload))
                .thenThrow(new JsonProcessingException("boom") {});

        // when
        CompletableFuture<SendResult<String, String>> future =
                kafkaEventSender.send("topic", "key", payload);

        // then
        assertThat(future).isCompletedExceptionally();

        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
    }

    private String headerValue(ProducerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
