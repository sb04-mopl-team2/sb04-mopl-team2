package com.codeit.mopl.batch.eventretry.config.step;

import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.Status;
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
public class PendingEventRetryStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    //
    private final FollowService followService;
    private final FollowRepository followRepository;

    /*
     *  팔로워 증가 재시도 Step
     * */
    @Bean
    public Step retryFollowerIncreaseStep() {
        return new StepBuilder("retryFollowerIncreaseStep", jobRepository)
                .tasklet(retryFollowerIncreaseTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet retryFollowerIncreaseTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== 팔로워 증가 재시도 시작 ===");
            int failureCount = 0;

            // PENDING 상태인 팔로우 객체 조회
            List<Follow> follows = followRepository.findByStatus(Status.PENDING);

            if (follows.isEmpty()) {
                log.info("[배치] PENDING 상태인 팔로우 객체가 없습니다: follows = {}", follows);
                return RepeatStatus.FINISHED;
            }
            int totalCount = follows.size();
            log.info("[배치] PENDING 상태인 팔로우 객체를 찾았습니다: total = {}", totalCount);

            // 팔로워 증가 재시도
            for (Follow follow : follows) {
                try {
                    log.info("[배치] PENDING 상태인 팔로우 객체의 팔로워 증가를 재시도 합니다: follow = {}", follow);
                    UUID followId = follow.getId();
                    UUID followeeId = follow.getFollowee().getId();
                    followService.processFollowerIncrease(followId, followeeId);
                    follow.setStatus(Status.CONFIRM);
                    follow.setRetryCount(0);

                } catch (Exception e) {
                    log.error("[배치] 팔로워 증가 재시도 실패: pendingFollow = {}, errorMessage = {}", follow, e.getMessage(), e);
                    follow.increaseRetryCount();

                    if (follow.getRetryCount() == Follow.MAX_RETRY_COUNT) {
                        follow.setStatus(Status.FAILED);
                        log.warn("[배치] 최대 재시도 횟수 초과로 FAILED 상태로 전환: follow = {}", follow);
                    }
                    failureCount++;
                }
                followRepository.save(follow);
            }

            log.info("[배치] 팔로워 증가 재시도 처리 결과: total = {}, failure = {}", totalCount, failureCount);
            log.info("=== 팔로워 증가 재시도 완료 ===");
            return RepeatStatus.FINISHED;
        };
    }
}
