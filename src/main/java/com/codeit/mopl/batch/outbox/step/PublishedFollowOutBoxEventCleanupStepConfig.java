package com.codeit.mopl.batch.outbox.step;

import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.repository.FollowOutBoxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PublishedFollowOutBoxEventCleanupStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    //
    private final FollowOutBoxEventRepository followOutBoxEventRepository;

    /**
    *   PUBLISHED 상태의 FollowOutBoxEvent 객체 삭제 Step
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
            // PUBLISHED 상태인 FollowOutBoxEvent 객체 조회
            List<FollowOutBoxEvent> events = followOutBoxEventRepository.findByOutBoxStatus(OutBoxStatus.PUBLISHED);

            if (events.isEmpty()) {
                log.info("[배치] PUBLISHED 상태인 FollowOutBoxEvent 객체가 없습니다: events = {}", events);
                return RepeatStatus.FINISHED;
            }
            log.info("[배치] PUBLISHED 상태인 FollowOutBoxEvent 객체를 찾았습니다: events = {}", events);
            int totalCount = events.size();

            // FollowOutBoxEvent 객체 삭제 (created_at 오름차순 정렬 기준 1000개)
            followOutBoxEventRepository.deleteTop1000ByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus.PUBLISHED);

            log.info("[배치] PUBLISHED 상태의 FollowOutBoxEvent 객체 삭제 결과: totalCount = {}", totalCount);
            return RepeatStatus.FINISHED;
        });
    }
}
