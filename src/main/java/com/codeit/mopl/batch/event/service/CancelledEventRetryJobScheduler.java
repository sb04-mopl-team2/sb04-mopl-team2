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
public class CancelledEventRetryJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job retryFollowerDecreaseJob;

    /*
    *  팔로워 감소 재시도 스케줄러
    * */
    @Scheduled(cron = "0 */5 * * * *")
    public void runRetryFollowerDecreaseJob() {
        try {
            log.info("=== 팔로워 감소 재시도 시작 ===");

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(retryFollowerDecreaseJob, params);
            log.info("=== 팔로워 감소 재시도 완료 ===");

        } catch (Exception e) {
            log.error("[배치] 팔로워 감소 재시도 실패: errorMessage = {}", e.getMessage(), e);
        }
    }
}
