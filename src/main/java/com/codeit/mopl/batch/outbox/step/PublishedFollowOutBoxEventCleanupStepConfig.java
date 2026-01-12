package com.codeit.mopl.batch.outbox.step;

import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.repository.OutBoxEventRepository;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PublishedFollowOutBoxEventCleanupStepConfig {

    private final int BATCH_SIZE = 1000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    //
    private final OutBoxEventRepository outBoxEventRepository;

    /**
     * PUBLISHED 상태의 FollowOutBoxEvent 객체 삭제 Step
     */
    @Bean
    public Step publishedFollowOutBoxEventStep() {
        return new StepBuilder("publishedFollowOutBoxEventStep", jobRepository)
                .tasklet(publihsedFollowOutBoxEventTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet publihsedFollowOutBoxEventTasklet() {
        return ((contribution, chunkContext) -> {
            // PUBLISHED 상태인 OutBox 이벤트 목록 조회 (created_at 오름차순 정렬 기준 1000개)
            List<OutBoxEvent> events = outBoxEventRepository.findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.PUBLISHED, PageRequest.of(0, BATCH_SIZE));

            if (events.isEmpty()) {
                log.info("[배치] PUBLISHED 상태인 OutBox 이벤트 객체가 없습니다.");
                return RepeatStatus.FINISHED;
            }
            int totalCount = events.size();
            log.info("[배치] PUBLISHED 상태인 OutBox 이벤트 객체를 찾았습니다: totalCount = {}", totalCount);

            // OutBox 이벤트 제거
            outBoxEventRepository.deleteAll(events);

            log.info("[배치] PUBLISHED 상태의 OutBox 이벤트 삭제 결과: totalCount = {}", totalCount);
            return RepeatStatus.FINISHED;
        });
    }
}
