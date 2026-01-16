package com.codeit.mopl.batch.outbox.scheduler;

import com.codeit.mopl.outbox.publisher.OutBoxKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublishFailedEventsJobScheduler {

    private final OutBoxKafkaPublisher publisher;
    
    /** 
     *  FAILED OutBoxEvent 1분 마다 퍼블리싱
     * */
    @Scheduled(cron = "0 * * * * *")
    public void publish() {
        publisher.publishFailedEvents();
    }
}
