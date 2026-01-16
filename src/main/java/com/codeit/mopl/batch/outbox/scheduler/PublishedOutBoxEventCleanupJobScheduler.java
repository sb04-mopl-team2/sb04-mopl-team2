package com.codeit.mopl.batch.outbox.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishedOutBoxEventCleanupJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job publishedOutBoxEventCleanupJob;

    /**
     *   PUBLISHED 상태의 FollowOutBoxEvent 객체 삭제 Scheduler
     */
    @Scheduled(cron = "0 0 14 * * Wed")
    public void runPublishedOutBoxEventCleanupJob() {
        try {
            log.info("=== PUBLISHED 상태의 OutBox 삭제 시작 ===");
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(publishedOutBoxEventCleanupJob, params);
            log.info("=== PUBLISHED 상태의 OutBox 삭제 완료 ===");
        } catch (Exception e) {
            log.error("[배치] PUBLISHED 상태의 OutBox 삭제 실패: errorMessage = {}", e.getMessage(), e);
        }
    }
}
