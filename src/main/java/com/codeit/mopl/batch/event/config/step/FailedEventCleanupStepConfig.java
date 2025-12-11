package com.codeit.mopl.batch.event.config.step;

import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
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
public class FailedEventCleanupStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    //
    private final FollowRepository followRepository;

    /*
     *  FAILED 팔로우 삭제 Step
     * */
    @Bean
    public Step failedFollowCleanupStep() {
        return new StepBuilder("failedFollowCleanupStep", jobRepository)
                .tasklet(failedFollowCleanupTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet failedFollowCleanupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== FAILED 팔로우 삭제 시작 ===");

            // FAILED 상태인 팔로우 객체 조회
            List<Follow> follows = followRepository.findByStatus(FollowStatus.FAILED);

            if (follows.isEmpty()) {
                log.info("[배치] FAILED 상태인 팔로우 객체가 없습니다: follows = {}", follows);
                return RepeatStatus.FINISHED;
            }
            log.info("[배치] FAILED 상태인 팔로우 객체를 찾았습니다: follows = {}", follows);
            int totalCount = follows.size();

            // 팔로우 객체 삭제
            followRepository.deleteAll(follows);

            log.info("[배치] FAILED 상태인 팔로우 객체 삭제 처리 결과: total = {}", totalCount);
            log.info("=== FAILED 팔로우 삭제 완료 ===");
            return RepeatStatus.FINISHED;
        };
    }
}
