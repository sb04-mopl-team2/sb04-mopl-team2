package com.codeit.mopl.batch.eventretry.config.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CancelledEventRetryJobConfig {

    private final JobRepository jobRepository;
    private Step retryFollowerDecreaseStep;

    /*
    *  팔로워 감소 재시도 Job
    * */
    @Bean
    public Job retryFollowerDecreaseJob() {
        return new JobBuilder("retryFollowerDecreaseJob", jobRepository)
                .start(retryFollowerDecreaseStep)
                .build();
    }
}
