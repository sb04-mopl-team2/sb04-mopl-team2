package com.codeit.mopl.batch;

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
public class ContentJobScheduler {

  private final JobLauncher jobLauncher;
  private final Job dailyMovieUpdateJob;
  private final Job dailyTvUpdateJob;
  private final Job dailySportsUpdateJob;

  /**
   * 매일 새벽 4시에 Movie, TV, Sports를 순차적으로 실행
   */
  @Scheduled(cron = "0 30 8 * * *")
  public void runDailyContentUpdate() {
    log.info("=== 일일 컨텐츠 업데이트 시작 (Movie + TV + Sports) ===");

    try {
      // 1. Movie Job 실행
      log.info(">>> 1단계: 영화 데이터 수집 시작");
      JobParameters movieParams = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis())
          .addString("contentType", "MOVIE")
          .toJobParameters();

      jobLauncher.run(dailyMovieUpdateJob, movieParams);
      log.info(">>> 1단계: 영화 데이터 수집 완료");

      // 2. API Rate Limit 고려하여 잠시 대기
      Thread.sleep(2000);

      // 3. TV Job 실행
      log.info(">>> 2단계: TV 프로그램 데이터 수집 시작");
      JobParameters tvParams = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis())
          .addString("contentType", "TV")
          .toJobParameters();

      jobLauncher.run(dailyTvUpdateJob, tvParams);
      log.info(">>> 2단계: TV 프로그램 데이터 수집 완료");

      // 4. API Rate Limit 고려하여 잠시 대기
      Thread.sleep(2000);

      // 5. Sports Job 실행
      log.info(">>> 3단계: 축구 경기 데이터 수집 시작");
      JobParameters sportsParams = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis())
          .addString("contentType", "SPORTS")
          .toJobParameters();

      jobLauncher.run(dailySportsUpdateJob, sportsParams);
      log.info(">>> 3단계: 축구 경기 데이터 수집 완료");

      log.info("=== 일일 컨텐츠 업데이트 완료 (Movie + TV + Sports) ===");

    } catch (Exception e) {
      log.error("일일 컨텐츠 업데이트 실패", e);
      throw new RuntimeException("일일 컨텐츠 업데이트 실패", e);
    }
  }
}