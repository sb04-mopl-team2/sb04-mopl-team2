package com.codeit.mopl.batch.tmdb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieJobScheduler {

  private final JobLauncher jobLauncher;
  private final Job initialMovieLoadJob;
  private final Job dailyMovieUpdateJob;

  @Value("${batch.initial-load.enabled:false}")
  private boolean initialLoadEnabled;

  private boolean initialLoadCompleted = false;

  /**
   * 애플리케이션 시작 시 최초 데이터 로드 실행
   * application.yml에서 batch.initial-load.enabled=true로 설정 시에만 실행
   */
  @EventListener(ApplicationReadyEvent.class)
  public void runInitialMovieLoad() {
    if (!initialLoadEnabled || initialLoadCompleted) {
      log.info("최초 영화 데이터 수집 건너뜀 (enabled={}, completed={})",
          initialLoadEnabled, initialLoadCompleted);
      return;
    }

    try {
      log.info("=== 최초 영화 데이터 수집 Job 시작 ===");

      JobParameters params = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(initialMovieLoadJob, params);
      initialLoadCompleted = true;

      log.info("=== 최초 영화 데이터 수집 Job 완료 ===");
    } catch (Exception e) {
      log.error("최초 영화 데이터 수집 실패", e);
    }
  }

  /**
   * 매일 자정 1시에 다음날 개봉 예정 영화 수집
   * 원하는 시간대로 조정 가능 (예: "0 0 1 * * *" = 매일 1시)
   */
  @Scheduled(cron = "0 * * * * *")
  public void runDailyMovieUpdate() {
    try {
      log.info("=== 일일 영화 업데이트 Job 시작 ===");

      JobParameters params = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(dailyMovieUpdateJob, params);

      log.info("=== 일일 영화 업데이트 Job 완료 ===");
    } catch (Exception e) {
      log.error("일일 영화 업데이트 실패", e);
    }
  }
}
