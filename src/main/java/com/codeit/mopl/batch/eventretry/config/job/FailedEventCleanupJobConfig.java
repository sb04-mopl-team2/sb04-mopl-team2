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
public class FailedEventCleanupJobConfig {

    private final JobRepository jobRepository;
    private final Step failedFollowCleanupStep;

    /*
    *  FAILED 팔로우 객체 삭제 Job
    * */
    @Bean
    public Job failedFollowCleanupJob() {
        return new JobBuilder("failedFollowCleanupJob", jobRepository)
                .start(failedFollowCleanupStep)
                .build();
    }
}
