package com.codeit.mopl.batch.event.service;

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
public class FailedEventCleanupJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job failedFollowCleanupJob;

    /*
    *  FAILED 팔로우 삭제 스케줄러
    * */
    @Scheduled(cron = "0 0 3 * * *")
    public void runFailedFollowCleanupJob() {
        try {
            log.info("=== FAILED 팔로우 삭제 시작 ===");

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(failedFollowCleanupJob, params);
            log.info("=== FAILED 팔로우 삭제 완료 ===");
        } catch (Exception e) {
            log.error("[배치] FAILED 팔로우 삭제 실패: errorMessage = {}", e.getMessage(), e);
        }
    }
}
