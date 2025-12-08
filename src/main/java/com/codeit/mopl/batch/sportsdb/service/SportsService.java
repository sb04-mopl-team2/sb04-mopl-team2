package com.codeit.mopl.batch.sportsdb.service;

import com.codeit.mopl.exception.batch.BatchErrorCode;
import com.codeit.mopl.exception.batch.InitialDataLoadException;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsService {

  private final SportsDbApiService sportsDbApiService;

  /**
   * 축구 경기 초기 데이터 로드 (6개월치)
   */
  public void runInitialDataLoad() {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusMonths(6);

    log.info("=== 축구 경기 초기 데이터 수집 시작 ===");
    log.info("수집 기간: {} ~ {}", startDate, endDate);

    int totalDays = 0;
    int totalEvents = 0;
    LocalDate currentDate = startDate;

    try {
      while (!currentDate.isAfter(endDate)) {
        log.info("수집 중인 날짜: {}", currentDate);

        // 해당 날짜의 축구 경기 조회 및 저장
        var savedContents = sportsDbApiService.fetchSoccerEventsByDate(currentDate).block();

        if (savedContents != null) {
          int eventCount = savedContents.size();
          totalEvents += eventCount;
          log.info("날짜 {}의 경기 수: {}", currentDate, eventCount);
        } else {
          log.warn("날짜 {}의 데이터를 가져올 수 없습니다.", currentDate);
        }

        totalDays++;
        currentDate = currentDate.plusDays(1);

        // API Rate Limit 고려 - 매일마다 1초 대기
        Thread.sleep(1000);
      }

      log.info("=== 축구 경기 초기 데이터 수집 완료 ===");
      log.info("총 처리 일수: {}", totalDays);
      log.info("총 수집 경기 수: {}", totalEvents);

    } catch (Exception e) {
      log.error("축구 경기 초기 데이터 수집 실패", e);
      throw new InitialDataLoadException(
          BatchErrorCode.INITIAL_DATA_LOAD_FAILED,
          Map.of("errorMessage", e.getMessage())
      );
    }
  }
}