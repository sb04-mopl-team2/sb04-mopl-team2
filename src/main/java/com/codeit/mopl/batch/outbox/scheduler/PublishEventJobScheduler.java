package com.codeit.mopl.batch.outbox.scheduler;

import com.codeit.mopl.outbox.publisher.OutBoxKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublishEventJobScheduler {

    private final OutBoxKafkaPublisher publisher;
    
    /** 
     *  FollowOutBoxEvent 10초마다 퍼블리싱
     * */
    @Scheduled(cron = "10 * * * * *")
    public void publish() {
        publisher.publishEvents();
    }
}
