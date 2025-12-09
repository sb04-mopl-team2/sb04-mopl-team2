package com.codeit.mopl.batch.event.config.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PendingEventRetryJobConfig {

    private final JobRepository jobRepository;
    private final Step retryFollowerIncreaseStep;

    /*
    *   팔로워 증가 재시도 Job
    * */
    @Bean
    public Job retryFollowerIncreaseJob() {
        return new JobBuilder("retryFollowerIncreaseJob", jobRepository)
                .start(retryFollowerIncreaseStep)
                .build();
    }

}
