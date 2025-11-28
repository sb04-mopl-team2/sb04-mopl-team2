package com.codeit.mopl.batch.tmdb.config;

import com.codeit.mopl.batch.tmdb.dto.TmdbDiscoverMovieResponse;
import com.codeit.mopl.batch.tmdb.service.TmdbApiService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MovieStepConfig {

  private final TmdbApiService tmdbApiService;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  /**
   * 매일 다음날 개봉 예정 영화 수집 Step
   */
  @Bean
  public Step dailyMovieUpdateStep() {
    return new StepBuilder("dailyMovieUpdateStep", jobRepository)
        .tasklet(dailyMovieUpdateTasklet(), transactionManager)
        .build();
  }

  @Bean
  public Tasklet dailyMovieUpdateTasklet() {
    return (contribution, chunkContext) -> {
      // 다음날 개봉 예정작 조회
      LocalDate tomorrow = LocalDate.now().plusDays(1);

      log.info("=== 일일 영화 데이터 업데이트 시작 ===");
      log.info("수집 대상 날짜: {}", tomorrow);

      // 첫 페이지 호출하면서 총 페이지 수도 함께 확인
      TmdbDiscoverMovieResponse firstPageResponse =
          tmdbApiService.discoverMoviesFromDateWithResponse(tomorrow, 1).block();

      if (firstPageResponse == null || firstPageResponse.getTotalPages() == 0) {
        log.warn("날짜 {}의 개봉 예정작이 없습니다.", tomorrow);
        return RepeatStatus.FINISHED;
      }

      int maxPages = Math.min(firstPageResponse.getTotalPages(), 500);
      log.info("총 페이지: {} (수집 예정: {})", firstPageResponse.getTotalPages(), maxPages);

      // 2페이지부터 나머지 수집
      for (int page = 2; page <= maxPages; page++) {
        tmdbApiService.discoverMoviesFromDate(tomorrow, page).block();
        Thread.sleep(1000);
      }

      log.info("=== 일일 영화 데이터 업데이트 완료 ===");
      log.info("총 처리 페이지 수: {}", maxPages);

      return RepeatStatus.FINISHED;
    };
  }
}