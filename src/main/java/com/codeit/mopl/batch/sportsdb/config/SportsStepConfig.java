package com.codeit.mopl.batch.sportsdb.config;

import com.codeit.mopl.batch.sportsdb.service.SportsDbApiService;
import com.codeit.mopl.domain.content.entity.Content;
import java.time.LocalDate;
import java.util.List;
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
public class SportsStepConfig {

  private final SportsDbApiService sportsDbApiService;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  /**
   * 매일 다음날 축구 경기 정보 수집 Step
   */
  @Bean
  public Step dailySportsUpdateStep() {
    return new StepBuilder("dailySportsUpdateStep", jobRepository)
        .tasklet(dailySportsUpdateTasklet(), transactionManager)
        .build();
  }

  @Bean
  public Tasklet dailySportsUpdateTasklet() {
    return (contribution, chunkContext) -> {
      // 다음날 경기 정보 조회
      LocalDate tomorrow = LocalDate.now().plusDays(1);

      log.info("=== 일일 축구 경기 데이터 업데이트 시작 ===");
      log.info("수집 대상 날짜: {}", tomorrow);

      // 해당 날짜의 축구 경기 조회 및 저장
      List<Content> savedContents = sportsDbApiService.fetchSoccerEventsByDate(tomorrow).block();

      if (savedContents == null || savedContents.isEmpty()) {
        log.warn("날짜 {}의 축구 경기 정보가 없습니다.", tomorrow);
      } else {
        log.info("저장된 경기 수: {}", savedContents.size());
      }

      log.info("=== 일일 축구 경기 데이터 업데이트 완료 ===");

      return RepeatStatus.FINISHED;
    };
  }
}