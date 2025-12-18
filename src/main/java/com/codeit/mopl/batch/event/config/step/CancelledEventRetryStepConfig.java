package com.codeit.mopl.batch.event.config.step;

import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.follow.service.FollowService;
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
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CancelledEventRetryStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    //
    private final FollowService followService;
    private final FollowRepository followRepository;

    /*
     *  팔로워 감소 재시도 Step
     * */
    @Bean
    public Step retryFollowerDecreaseStep() {
        return new StepBuilder("retryFollowerDecreaseStep", jobRepository)
                .tasklet(retryFollowerDecreaseTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet retryFollowerDecreaseTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== 팔로워 감소 재시도 시작 ===");
            int failureCount = 0;

            // CANCELLED 상태인 팔로우 객체 조회
            List<Follow> follows = followRepository.findByStatus(FollowStatus.CANCELLED);

            if (follows.isEmpty()) {
                log.info("[배치] CANCELLED 상태인 팔로우 객체가 없습니다: follows = {}", follows);
                return RepeatStatus.FINISHED;
            }
            log.info("[배치] CANCELLED 상태인 팔로우 객체를 찾았습니다: follows = {}", follows);
            int totalCount = follows.size();

            // 팔로워 감소 재시도
            for (Follow follow : follows) {
                try {
                    log.info("[배치] CANCELLED 상태인 팔로우 객체의 팔로워 감소를 재시도 합니다: follow = {}", follow);
                    UUID followId = follow.getId();
                    UUID followeeId = follow.getFollowee().getId();
                    followService.processFollowerDecrease(followId, followeeId); // 팔로워 감소 + 팔로우 객체 삭제 수행

                } catch (Exception e) {
                    follow.increaseRetryCount();
                    log.error(
                            "[배치] 팔로워 감소 재시도 실패 (retryCount = {}): followId = {}, followeeId = {}, errorMessage = {}",
                            follow.getRetryCount(),
                            follow.getId(),
                            follow.getFollowee().getId(),
                            e.getMessage(),
                            e
                    );
                    followRepository.save(follow);
                    failureCount++;
                }
            }
            log.info("[배치] 팔로워 감소 재시도 처리 결과: total = {}, failure = {}", totalCount, failureCount);
            log.info("=== 팔로워 감소 재시도 완료 ===");
            return RepeatStatus.FINISHED;
        };
    }
}
