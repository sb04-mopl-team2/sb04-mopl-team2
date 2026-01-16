package com.codeit.mopl.batch.outbox.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PublishedOutBoxEventCleanupJobConfig {

    private final JobRepository jobRepository;
    private final Step publishedOutBoxEventStep;

    /**
    *   PUBLISHED 상태의 FollowOutBoxEvent 객체 삭제 Job
    */
    @Bean
    public Job publishedOutBoxEventCleanupJob() {
        return new JobBuilder("publishedOutBoxEventCleanupJob", jobRepository)
                .start(publishedOutBoxEventStep)
                .build();
    }
}
