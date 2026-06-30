package com.codeit.mopl.event.metrics;

import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KafkaEventStatsJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  public void upsertBatch10m(List<KafkaEventStatsDelta> deltas) {
    if (deltas == null || deltas.isEmpty()) return;

    StringBuilder sql = new StringBuilder("""
      insert into kafka_event_stats_10m
        (topic, event_type, bucket_time, total_count, fail_count, dup_count, updated_at)
      values
    """);

    for (int i = 0; i < deltas.size(); i++) {
      if (i > 0) sql.append(",");
      sql.append("(?, ?, ?, ?, ?, ?, now())");
    }

    sql.append("""
      on conflict (topic, event_type, bucket_time)
      do update set
        total_count = kafka_event_stats_10m.total_count + excluded.total_count,
        fail_count  = kafka_event_stats_10m.fail_count  + excluded.fail_count,
        dup_count   = kafka_event_stats_10m.dup_count   + excluded.dup_count,
        updated_at  = now()
    """);

    Object[] args = new Object[deltas.size() * 6];
    int idx = 0;

    for (KafkaEventStatsDelta d : deltas) {
      args[idx++] = d.topic();
      args[idx++] = d.eventType();
      args[idx++] = Timestamp.from(d.bucketTime());
      args[idx++] = d.totalDelta();
      args[idx++] = d.failDelta();
      args[idx++] = d.dupDelta();
    }

    jdbcTemplate.update(sql.toString(), args);
  }
}
