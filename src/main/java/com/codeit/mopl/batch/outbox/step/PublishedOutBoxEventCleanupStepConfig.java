package com.codeit.mopl.batch.outbox.step;

import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PublishedOutBoxEventCleanupStepConfig {

    private final int BATCH_SIZE = 1000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger lastDeletedCount = new AtomicInteger(0);
    //
    private final OutBoxEventRepository outBoxEventRepository;

    /**
     * PUBLISHED 상태의 OutBox 삭제 Step
     */
    @Bean
    public Step publishedOutBoxEventStep() {
        return new StepBuilder("publishedOutBoxEventStep", jobRepository)
                .tasklet(publihsedOutBoxEventTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet publihsedOutBoxEventTasklet() {
        // 누적 삭제 건수 Counter
        Counter deletedCounter = Counter.builder("outbox.cleanup.deleted.count")
                .description("PUBLISHED 상태 OutBox 누적 삭제 건수")
                .tag("status", "published")
                .tag("aggregate", "follow")
                .register(meterRegistry);

        // 최근 배치 삭제 건수 Gauge
        Gauge.builder("outbox.cleanup.deleted.last", lastDeletedCount, AtomicInteger::get)
                .description("최근 배치 실행에서 삭제된 PUBLISHED 상태 OutBox 삭제 건수")
                .tag("status", "published")
                .tag("aggregate", "follow")
                .register(meterRegistry);

        return ((contribution, chunkContext) -> {
            // PUBLISHED 상태인 OutBox 목록 조회 (created_at 오름차순 정렬 기준 1000개)
            List<OutBoxEvent> events = outBoxEventRepository.findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.PUBLISHED, PageRequest.of(0, BATCH_SIZE));

            if (events.isEmpty()) {
                log.info("[배치] PUBLISHED 상태인 OutBox가 없습니다.");
                return RepeatStatus.FINISHED;
            }
            int totalCount = events.size();
            log.info("[배치] PUBLISHED 상태인 OutBox를 찾았습니다: totalCount = {}", totalCount);

            // OutBox 이벤트 제거
            outBoxEventRepository.deleteAll(events);

            // 메트릭 기록
            deletedCounter.increment(totalCount);
            lastDeletedCount.set(totalCount);

            log.info("[배치] PUBLISHED 상태의 OutBox 삭제 결과: totalCount = {}", totalCount);
            return RepeatStatus.FINISHED;
        });
    }
}
