package com.codeit.mopl.event.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class KafkaEventStatsQueryRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Test
  void aggregateByBucketLastMinutes_shouldPassArgsAndMapBucketsAndRates() {
    // given
    KafkaEventStatsQueryRepository repo = new KafkaEventStatsQueryRepository(jdbcTemplate);

    Instant from = Instant.parse("2025-12-26T00:00:00Z");
    Instant to = Instant.parse("2025-12-26T01:00:00Z");
    Instant bucketStart = Instant.parse("2025-12-26T00:50:00Z");
    Instant bucketEnd = Instant.parse("2025-12-26T01:00:00Z");

    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), anyInt(), anyInt()))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          RowMapper<KafkaMetricsSummary> mapper =
              (RowMapper<KafkaMetricsSummary>) invocation.getArgument(1);

          java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);

          when(rs.getTimestamp("from_utc")).thenReturn(Timestamp.from(from));
          when(rs.getTimestamp("to_utc")).thenReturn(Timestamp.from(to));

          when(rs.getTimestamp("bucket_start_utc")).thenReturn(Timestamp.from(bucketStart));
          when(rs.getTimestamp("bucket_end_utc")).thenReturn(Timestamp.from(bucketEnd));

          when(rs.getString("topic")).thenReturn("topicA");
          when(rs.getString("event_type")).thenReturn("EVENT_A");

          when(rs.getLong("total")).thenReturn(10L);
          when(rs.getLong("fail")).thenReturn(2L);
          when(rs.getLong("dup")).thenReturn(3L);

          return List.of(mapper.mapRow(rs, 0));
        });

    // when
    List<KafkaMetricsSummary> result = repo.aggregateByBucketLastMinutes(60);

    // then: args가 minutes, BUCKET_MINUTES(10)로 전달되는지
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(60), eq(10));

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("from kafka_event_stats_10m");
    assertThat(sql).contains("group by s.bucket_time, s.topic, s.event_type");
    assertThat(sql).contains("order by bucket_start_utc desc, total desc");

    // then: 매핑 및 rate 계산
    assertThat(result).hasSize(1);
    KafkaMetricsSummary s = result.get(0);

    assertThat(s.topic()).isEqualTo("topicA");
    assertThat(s.eventType()).isEqualTo("EVENT_A");

    assertThat(s.fromUtc()).isEqualTo(from);
    assertThat(s.toUtc()).isEqualTo(to);

    assertThat(s.bucketStartUtc()).isEqualTo(bucketStart);
    assertThat(s.bucketEndUtc()).isEqualTo(bucketEnd);

    assertThat(s.total()).isEqualTo(10L);
    assertThat(s.fail()).isEqualTo(2L);
    assertThat(s.dup()).isEqualTo(3L);

    assertThat(s.failureRate()).isEqualTo(0.2);
    assertThat(s.duplicateRate()).isEqualTo(0.3);
  }

  @Test
  void aggregateLastMinutes_shouldReturnNullBuckets_andRatesZeroWhenTotalZero() {
    // given
    KafkaEventStatsQueryRepository repo = new KafkaEventStatsQueryRepository(jdbcTemplate);

    Instant from = Instant.parse("2025-12-26T00:00:00Z");
    Instant to = Instant.parse("2025-12-26T01:00:00Z");

    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), anyInt()))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          RowMapper<KafkaMetricsSummary> mapper =
              (RowMapper<KafkaMetricsSummary>) invocation.getArgument(1);

          java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);

          when(rs.getString("topic")).thenReturn("topicB");
          when(rs.getString("event_type")).thenReturn("EVENT_B");

          when(rs.getTimestamp("from_utc")).thenReturn(Timestamp.from(from));
          when(rs.getTimestamp("to_utc")).thenReturn(Timestamp.from(to));

          // total=0이면 failureRate/duplicateRate는 0.0이어야 함
          when(rs.getLong("total")).thenReturn(0L);
          when(rs.getLong("fail")).thenReturn(5L);
          when(rs.getLong("dup")).thenReturn(7L);

          return List.of(mapper.mapRow(rs, 0));
        });

    // when
    List<KafkaMetricsSummary> result = repo.aggregateLastMinutes(15);

    // then: args 전달 확인
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(15));

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("from kafka_event_stats_10m");
    assertThat(sql).contains("group by s.topic, s.event_type");
    assertThat(sql).contains("order by total desc");

    // then: bucket null + rate 0.0
    assertThat(result).hasSize(1);
    KafkaMetricsSummary s = result.get(0);

    assertThat(s.topic()).isEqualTo("topicB");
    assertThat(s.eventType()).isEqualTo("EVENT_B");

    assertThat(s.fromUtc()).isEqualTo(from);
    assertThat(s.toUtc()).isEqualTo(to);

    assertThat(s.bucketStartUtc()).isNull();
    assertThat(s.bucketEndUtc()).isNull();

    assertThat(s.total()).isEqualTo(0L);
    assertThat(s.fail()).isEqualTo(5L);
    assertThat(s.dup()).isEqualTo(7L);

    assertThat(s.failureRate()).isEqualTo(0.0);
    assertThat(s.duplicateRate()).isEqualTo(0.0);
  }
}
