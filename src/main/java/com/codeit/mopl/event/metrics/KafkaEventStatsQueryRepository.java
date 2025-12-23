package com.codeit.mopl.event.metrics;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KafkaEventStatsQueryRepository {

  private final JdbcTemplate jdbcTemplate;

  public List<KafkaMetricsSummary> aggregateByBucketLastMinutes(int minutes) {

    final String sql = """
    with range as (
      select
        (now() at time zone 'utc') as to_utc,
        (now() at time zone 'utc') - (? * interval '1 minute') as from_utc
    )
    select
      s.bucket_time as bucket_start_utc,
      s.bucket_time + interval '10 minute' as bucket_end_utc,

      (s.bucket_time at time zone 'Asia/Seoul') as bucket_start_kst,
      ((s.bucket_time + interval '10 minute') at time zone 'Asia/Seoul') as bucket_end_kst,

      s.topic,
      s.event_type,
      r.from_utc as from_utc,
      r.to_utc   as to_utc,
      (r.from_utc at time zone 'Asia/Seoul') as from_kst,
      (r.to_utc   at time zone 'Asia/Seoul') as to_kst,

      sum(s.total_count) as total,
      sum(s.fail_count)  as fail,
      sum(s.dup_count)   as dup
    from kafka_event_stats_10m s
    cross join range r
    where s.bucket_time >= r.from_utc
    group by
      s.bucket_time, s.topic, s.event_type,
      r.from_utc, r.to_utc
    order by bucket_start_utc desc, total desc
    """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Instant fromUtc = rs.getTimestamp("from_utc").toInstant();
          Instant toUtc   = rs.getTimestamp("to_utc").toInstant();
          Instant fromKst = rs.getTimestamp("from_kst").toInstant();
          Instant toKst   = rs.getTimestamp("to_kst").toInstant();

          Instant bucketStartUtc = rs.getTimestamp("bucket_start_utc").toInstant();
          Instant bucketEndUtc   = rs.getTimestamp("bucket_end_utc").toInstant();
          Instant bucketStartKst = rs.getTimestamp("bucket_start_kst").toInstant();
          Instant bucketEndKst   = rs.getTimestamp("bucket_end_kst").toInstant();

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
              fromKst,
              toKst,
              bucketStartUtc,
              bucketEndUtc,
              bucketStartKst,
              bucketEndKst,
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

  public List<KafkaMetricsSummary> aggregateLastMinutes(int minutes) {

    final String sql = """
    with range as (
      select
        (now() at time zone 'utc') as to_utc,
        (now() at time zone 'utc') - (? * interval '1 minute') as from_utc
    )
    select
      s.topic,
      s.event_type,
      r.from_utc as from_utc,
      r.to_utc   as to_utc,
      (r.from_utc at time zone 'Asia/Seoul') as from_kst,
      (r.to_utc   at time zone 'Asia/Seoul') as to_kst,
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

          Instant fromKst = rs.getTimestamp("from_kst").toInstant();
          Instant toKst   = rs.getTimestamp("to_kst").toInstant();

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
              fromKst,
              toKst,
              null, null, null, null, // bucket 필드는 전체집계에선 null
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
