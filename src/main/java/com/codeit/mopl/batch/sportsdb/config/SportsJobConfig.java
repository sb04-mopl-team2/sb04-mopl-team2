package com.codeit.mopl.batch.sportsdb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SportsJobConfig {

  private final JobRepository jobRepository;
  private final Step dailySportsUpdateStep;

  /**
   * 매일 다음날 축구 경기 정보 수집 Job
   */
  @Bean
  public Job dailySportsUpdateJob() {
    return new JobBuilder("dailySportsUpdateJob", jobRepository)
        .start(dailySportsUpdateStep)
        .build();
  }
}