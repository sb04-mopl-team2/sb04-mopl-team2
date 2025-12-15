package com.codeit.mopl.batch.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 배치 작업의 메트릭을 수집하고 관리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchMetricsService {

  private final MeterRegistry meterRegistry;

  /**
   * 배치 작업 실행 기록 (성공/실패)
   *
   * @param jobName 작업 이름 (예: dailyMovieUpdateJob)
   * @param status  실행 상태 (SUCCESS, FAILED)
   */
  public void recordJobExecution(String jobName, JobStatus status) {
    meterRegistry.counter("batch.job.runs.total",
            "job", jobName,
            "status", status.name())
        .increment();

    log.info("[Metrics] Job 실행 기록: job={}, status={}", jobName, status);
  }

  /**
   * 배치 작업 실행 시간 기록
   *
   * @param jobName    작업 이름
   * @param durationMs 실행 시간 (밀리초)
   */
  public void recordJobDuration(String jobName, long durationMs) {
    meterRegistry.timer("batch.job.duration",
            "job", jobName)
        .record(durationMs, TimeUnit.MILLISECONDS);

    log.info("[Metrics] Job 실행 시간 기록: job={}, duration={}ms", jobName, durationMs);
  }

  /**
   * 수집된 컨텐츠 수 기록
   *
   * @param contentType 컨텐츠 타입 (MOVIE, TV, SPORTS)
   * @param count       수집된 개수
   */
  public void recordContentCollected(String contentType, int count) {
    meterRegistry.counter("batch.content.collected.total",
            "type", contentType)
        .increment(count);

    log.info("[Metrics] 컨텐츠 수집 기록: type={}, count={}", contentType, count);
  }

  /**
   * 배치 작업 상태 Enum
   */
  public enum JobStatus {
    SUCCESS,
    FAILED
  }
}