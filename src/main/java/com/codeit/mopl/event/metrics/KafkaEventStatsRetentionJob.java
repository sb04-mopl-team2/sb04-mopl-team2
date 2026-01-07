package com.codeit.mopl.event.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventStatsRetentionJob {

  private final JdbcTemplate jdbcTemplate;

  @Scheduled(cron = "0 0 3 * * *")
  public void cleanup14Days() {
    int deleted = jdbcTemplate.update("""
      delete from kafka_event_stats_10m
      where bucket_time < (now() at time zone 'utc') - interval '14 days'
    """);
    log.info("[KAFKA_STATS] retention cleanup deletedRows={}", deleted);
  }
}
