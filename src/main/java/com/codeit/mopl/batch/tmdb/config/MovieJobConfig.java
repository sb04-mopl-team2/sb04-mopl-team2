package com.codeit.mopl.batch.tmdb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MovieJobConfig {

  private final JobRepository jobRepository;
  private final Step dailyMovieUpdateStep;

  /**
   * 매일 다음날 개봉 예정 영화 수집 Job
   */
  @Bean
  public Job dailyMovieUpdateJob() {
    return new JobBuilder("dailyMovieUpdateJob", jobRepository)
        .start(dailyMovieUpdateStep)
        .build();
  }
}

