package com.codeit.mopl.batch.service;

import com.codeit.mopl.batch.service.BatchMetricsService.JobStatus;
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
  private final BatchMetricsService metricsService;

  /**
   * 매일 오전 8시 30분에 Movie, TV, Sports를 순차적으로 실행
   */
  @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
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
  /**
   * 개별 Job 실행 및 메트릭 수집
   */
  private void runJob(String jobName, Job job, String contentType) {
    long startTime = System.currentTimeMillis();

    try {
      log.info(">>> {}단계: {} 데이터 수집 시작",
          contentType.equals("MOVIE") ? "1" : contentType.equals("TV") ? "2" : "3",
          getJobDisplayName(contentType));

      JobParameters params = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis())
          .addString("contentType", contentType)
          .toJobParameters();

      jobLauncher.run(job, params);

      long duration = System.currentTimeMillis() - startTime;

      // 메트릭 기록: 성공
      metricsService.recordJobExecution(jobName, JobStatus.SUCCESS);
      metricsService.recordJobDuration(jobName, duration);

      log.info(">>> {}단계: {} 데이터 수집 완료 ({}ms)",
          contentType.equals("MOVIE") ? "1" : contentType.equals("TV") ? "2" : "3",
          getJobDisplayName(contentType), duration);

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;

      // 메트릭 기록: 실패
      metricsService.recordJobExecution(jobName, JobStatus.FAILED);
      metricsService.recordJobDuration(jobName, duration);

      log.error(">>> {} 데이터 수집 실패", getJobDisplayName(contentType), e);
      throw new RuntimeException(jobName + " 실행 실패", e);
    }
  }

  private String getJobDisplayName(String contentType) {
    return switch (contentType) {
      case "MOVIE" -> "영화";
      case "TV" -> "TV 프로그램";
      case "SPORTS" -> "축구 경기";
      default -> contentType;
    };
  }
}