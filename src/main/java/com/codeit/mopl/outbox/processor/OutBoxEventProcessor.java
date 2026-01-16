package com.codeit.mopl.outbox.processor;

import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
import com.codeit.mopl.outbox.util.ErrorMessageSummarizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * 비동기 Kafka send의 callback은 별도의 스레드로 실행
 * 기존 핸들러의 트랜잭션을 전파받지 못함 -> 트랜잭션 없이 save() 수행 우려됨
 * 이에 따라 전파 옵션을 REQUIRES_NEW로 설정하여
 * 핸들러의 트랜잭션과 관계 없이 안전하게 DB 반영
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutBoxEventProcessor {

    private final OutBoxEventRepository outBoxEventRepository;

    /**
     * Kafka 전송 결과 처리
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void processKafkaResult(OutBoxEvent event, Throwable ex) {
        if (ex != null) {
            handleFailure(event, ex.getMessage());
        } else {
            event.markPublished();
        }
        outBoxEventRepository.save(event);
    }

    /**
     * 역직렬화 실패나 기타 구조적 문제 처리
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void handleDeserializationFailure(OutBoxEvent event, String errorMessage) {
        String summarized = ErrorMessageSummarizer.summarizeErrorMessage(errorMessage);
        log.error("[OutBox] payload 역직렬화 실패: outBoxEventId = {}, errorMessage = {}", event.getId(), summarized);
        event.markDead(summarized);
        outBoxEventRepository.save(event);
    }

    /**
     * 공통 실패 처리
     */
    private void handleFailure(OutBoxEvent event, String errorMessage) {
        String summarized = ErrorMessageSummarizer.summarizeErrorMessage(errorMessage);
        if (event.getRetryCount() >= OutBoxEvent.MAX_RETRY_COUNT) {
            event.markDead(summarized);
        } else {
            event.markFailed(summarized);
        }
    }
}
