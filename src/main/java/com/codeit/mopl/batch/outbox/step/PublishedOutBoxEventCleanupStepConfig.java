package com.codeit.mopl.batch.outbox.step;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PublishedOutBoxEventCleanupStepConfig {

    @Value("${outbox.batch.cleanup.size}")
    private int batchSize;

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
                .tasklet(publishedOutBoxEventTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet publishedOutBoxEventTasklet() {
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
            // OutBox 이벤트 제거
            int totalCount = outBoxEventRepository.deletePublishedBatch(batchSize);

            // 메트릭 기록
            deletedCounter.increment(totalCount);
            lastDeletedCount.set(totalCount);

            log.info("[배치] PUBLISHED 상태의 OutBox 삭제 결과: totalCount = {}", totalCount);
            return RepeatStatus.FINISHED;
        });
    }
}
