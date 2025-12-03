package com.codeit.mopl.batch.tmdb.service;

import com.codeit.mopl.batch.tmdb.dto.TmdbDiscoverMovieResponse;
import com.codeit.mopl.exception.batch.BatchErrorCode;
import com.codeit.mopl.exception.batch.InitialDataLoadException;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
  private final TmdbApiService tmdbApiService;

  @Async("taskExecutor")
  public void runInitialDataLoad() {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusMonths(6);

    log.info("=== 초기 데이터 수집 시작 ===");
    log.info("수집 기간: {} ~ {}", startDate, endDate);

    int totalPages = 0;
    LocalDate currentDate = startDate;

    try {
      while (!currentDate.isAfter(endDate)) {
        log.info("수집 중인 날짜: {}", currentDate);

        // 첫 페이지 호출하면서 총 페이지 수 확인
        TmdbDiscoverMovieResponse firstPageResponse =
            tmdbApiService.discoverMoviesFromDateWithResponse(currentDate, 1).block();

        if (firstPageResponse == null || firstPageResponse.getTotalPages() == 0) {
          log.warn("날짜 {}의 데이터를 가져올 수 없습니다. 다음 날짜로 이동합니다.", currentDate);
          currentDate = currentDate.plusDays(1);
          continue;
        }

        totalPages++;
        int maxPages = Math.min(firstPageResponse.getTotalPages(), 500);
        log.info("날짜 {}의 총 페이지: {} (수집 예정: {})",
            currentDate, firstPageResponse.getTotalPages(), maxPages);

        // 2페이지부터 나머지 수집
        for (int page = 2; page <= maxPages; page++) {
          tmdbApiService.discoverMoviesFromDate(currentDate, page).block();
          totalPages++;

          // API Rate Limit 고려
          if (page % 10 == 0) {
            Thread.sleep(1000);
          }
        }

        currentDate = currentDate.plusDays(1);
      }

      log.info("=== 초기 데이터 수집 완료 ===");
      log.info("총 처리 페이지 수: {}", totalPages);

    } catch (Exception e) {
      log.error("초기 데이터 수집 실패", e);
      throw new InitialDataLoadException(
          BatchErrorCode.INITIAL_DATA_LOAD_FAILED,
          Map.of("errorMessage", e.getMessage())
      );
    }
  }
}
