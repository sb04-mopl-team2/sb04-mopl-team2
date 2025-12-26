package com.codeit.mopl.event.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

@ExtendWith(MockitoExtension.class)
class KafkaEventStatsJdbcRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Test
  void upsertBatch10m_shouldBuildSqlAndBindArgsInOrder() {
    // given
    KafkaEventStatsJdbcRepository repo = new KafkaEventStatsJdbcRepository(jdbcTemplate);

    Instant bucket = Instant.parse("2025-12-26T00:00:00Z");
    List<KafkaEventStatsDelta> deltas = List.of(
        new KafkaEventStatsDelta("topicA", "EVENT_A", bucket, 2, 1, 0),
        new KafkaEventStatsDelta("topicB", "EVENT_B", bucket, 5, 0, 3)
    );

    when(jdbcTemplate.update(any(String.class), any(Object[].class))).thenReturn(2);

    // when
    repo.upsertBatch10m(deltas);

    // then
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);

    verify(jdbcTemplate).update(sqlCaptor.capture(), argsCaptor.capture());

    String sql = sqlCaptor.getValue();
    Object[] args = argsCaptor.getValue();

    // SQL 형태 검증 (핵심 구문 포함 여부)
    assertThat(sql).contains("insert into kafka_event_stats_10m");
    assertThat(sql).contains("(topic, event_type, bucket_time, total_count, fail_count, dup_count, updated_at)");
    assertThat(sql).contains("on conflict (topic, event_type, bucket_time)");
    assertThat(sql).contains("do update set");

    // values 로우가 2개 들어갔는지(= "(?, ?, ?, ?, ?, ?, now())"가 2번)
    assertThat(countOccurrences(sql, "(?, ?, ?, ?, ?, ?, now())")).isEqualTo(2);

    // args 매핑 순서 검증: row당 6개씩 총 12개
    assertThat(args).hasSize(12);

    // 첫 번째 delta
    assertThat(args[0]).isEqualTo("topicA");
    assertThat(args[1]).isEqualTo("EVENT_A");
    assertThat(args[2]).isEqualTo(Timestamp.from(bucket));
    assertThat(args[3]).isEqualTo(2L);
    assertThat(args[4]).isEqualTo(1L);
    assertThat(args[5]).isEqualTo(0L);

    // 두 번째 delta
    assertThat(args[6]).isEqualTo("topicB");
    assertThat(args[7]).isEqualTo("EVENT_B");
    assertThat(args[8]).isEqualTo(Timestamp.from(bucket));
    assertThat(args[9]).isEqualTo(5L);
    assertThat(args[10]).isEqualTo(0L);
    assertThat(args[11]).isEqualTo(3L);
  }

  private static int countOccurrences(String s, String target) {
    int count = 0;
    int idx = 0;
    while ((idx = s.indexOf(target, idx)) != -1) {
      count++;
      idx += target.length();
    }
    return count;
  }
}
