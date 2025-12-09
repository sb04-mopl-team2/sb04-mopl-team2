package com.codeit.mopl.batch;

import com.codeit.mopl.batch.sportsdb.service.SportsService;
import com.codeit.mopl.batch.tmdb.movie.service.MovieService;
import com.codeit.mopl.batch.tmdb.tvseries.service.TvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentInitService {

  private final MovieService movieService;
  private final TvService tvService;
  private final SportsService sportsService;

  /**
   * Movie, TV, Sports 초기 데이터를 순차적으로 수집
   * 비동기 방식으로 백그라운드에서 실행
   */
  @Async("taskExecutor")
  public void runInitialDataLoad() {
    log.info("=== 전체 컨텐츠 초기 데이터 수집 시작 (Movie + TV + Sports) ===");

    try {
      // 1. Movie 초기 데이터 수집
      log.info(">>> 1단계: 영화 초기 데이터 수집 시작");
      movieService.runInitialDataLoad();
      log.info(">>> 1단계: 영화 초기 데이터 수집 완료");

      // 2. API Rate Limit 고려하여 잠시 대기
      Thread.sleep(5000);

      // 3. TV 초기 데이터 수집
      log.info(">>> 2단계: TV 프로그램 초기 데이터 수집 시작");
      tvService.runInitialDataLoad();
      log.info(">>> 2단계: TV 프로그램 초기 데이터 수집 완료");

      // 4. API Rate Limit 고려하여 잠시 대기
      Thread.sleep(5000);

      // 5. Sports 초기 데이터 수집
      log.info(">>> 3단계: 축구 경기 초기 데이터 수집 시작");
      sportsService.runInitialDataLoad();
      log.info(">>> 3단계: 축구 경기 초기 데이터 수집 완료");

      log.info("=== 전체 컨텐츠 초기 데이터 수집 완료 (Movie + TV + Sports) ===");

    } catch (Exception e) {
      log.error("전체 컨텐츠 초기 데이터 수집 실패", e);
    }
  }
}