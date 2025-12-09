package com.codeit.mopl.batch.tmdb.tvseries.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TvJobConfig {

  private final JobRepository jobRepository;
  private final Step dailyTvUpdateStep;

  /**
   * 매일 다음날 방영 예정 TV 프로그램 수집 Job
   */
  @Bean
  public Job dailyTvUpdateJob() {
    return new JobBuilder("dailyTvUpdateJob", jobRepository)
        .start(dailyTvUpdateStep)
        .build();
  }
}