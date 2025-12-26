package com.codeit.mopl.event.metrics;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KafkaEventStatsQueryRepository {

  private static final int BUCKET_MINUTES = 10;

  private final JdbcTemplate jdbcTemplate;

  public List<KafkaMetricsSummary> aggregateByBucketLastMinutes(int minutes) {

    final String sql = """
      with range as (
        select
          now() as to_utc,
          now() - (? * interval '1 minute') as from_utc
      )
      select
        s.bucket_time as bucket_start_utc,
        s.bucket_time + (? * interval '1 minute') as bucket_end_utc,
        s.topic,
        s.event_type,
        r.from_utc as from_utc,
        r.to_utc   as to_utc,
        sum(s.total_count) as total,
        sum(s.fail_count)  as fail,
        sum(s.dup_count)   as dup
      from kafka_event_stats_10m s
      cross join range r
      where s.bucket_time >= r.from_utc
      group by s.bucket_time, s.topic, s.event_type, r.from_utc, r.to_utc
      order by bucket_start_utc desc, total desc
      """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Instant fromUtc = rs.getTimestamp("from_utc").toInstant();
          Instant toUtc   = rs.getTimestamp("to_utc").toInstant();

          Instant bucketStartUtc = rs.getTimestamp("bucket_start_utc").toInstant();
          Instant bucketEndUtc   = rs.getTimestamp("bucket_end_utc").toInstant();

          String topic = rs.getString("topic");
          String eventType = rs.getString("event_type");

          long total = rs.getLong("total");
          long fail  = rs.getLong("fail");
          long dup   = rs.getLong("dup");

          double failRate = (total == 0) ? 0.0 : (double) fail / total;
          double dupRate  = (total == 0) ? 0.0 : (double) dup / total;

          return new KafkaMetricsSummary(
              topic,
              eventType,
              fromUtc,
              toUtc,
              bucketStartUtc,
              bucketEndUtc,
              total,
              fail,
              dup,
              failRate,
              dupRate
          );
        },
        minutes,
        BUCKET_MINUTES
    );
  }

  public List<KafkaMetricsSummary> aggregateLastMinutes(int minutes) {

    final String sql = """
      with range as (
        select
          now() as to_utc,
          now() - (? * interval '1 minute') as from_utc
      )
      select
        s.topic,
        s.event_type,
        r.from_utc as from_utc,
        r.to_utc   as to_utc,
        sum(s.total_count) as total,
        sum(s.fail_count)  as fail,
        sum(s.dup_count)   as dup
      from kafka_event_stats_10m s
      cross join range r
      where s.bucket_time >= r.from_utc
      group by s.topic, s.event_type, r.from_utc, r.to_utc
      order by total desc
      """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          String topic = rs.getString("topic");
          String eventType = rs.getString("event_type");

          Instant fromUtc = rs.getTimestamp("from_utc").toInstant();
          Instant toUtc   = rs.getTimestamp("to_utc").toInstant();

          long total = rs.getLong("total");
          long fail  = rs.getLong("fail");
          long dup   = rs.getLong("dup");

          double failRate = (total == 0) ? 0.0 : (double) fail / total;
          double dupRate  = (total == 0) ? 0.0 : (double) dup / total;

          return new KafkaMetricsSummary(
              topic,
              eventType,
              fromUtc,
              toUtc,
              null,
              null,
              total,
              fail,
              dup,
              failRate,
              dupRate
          );
        },
        minutes
    );
  }
}
