package com.codeit.mopl.event.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaEventStats10mCollectorTest {

  @Mock
  private KafkaEventStatsJdbcRepository repo;

  @Test
  void flushToDb_shouldAggregateAndUpsert() {
    // given: 고정된 시간 (10분 버킷)
    Clock fixedClock =
        Clock.fixed(Instant.parse("2025-12-26T00:05:30Z"), ZoneOffset.UTC);

    KafkaEventStats10mCollector collector =
        new KafkaEventStats10mCollector(repo, fixedClock);

    KafkaEventKey key =
        new KafkaEventKey("mopl-user-role-update", "USER_ROLE_UPDATE");

    // when: 지표 누적
    collector.incTotal(key);
    collector.incTotal(key);
    collector.incFail(key);
    collector.incDup(key);

    collector.flushToDb();

    // then: DB upsert 검증
    ArgumentCaptor<List<KafkaEventStatsDelta>> captor =
        ArgumentCaptor.forClass(List.class);

    verify(repo).upsertBatch10m(captor.capture());

    List<KafkaEventStatsDelta> deltas = captor.getValue();
    assertThat(deltas).hasSize(1);

    KafkaEventStatsDelta d = deltas.get(0);
    assertThat(d.topic()).isEqualTo("mopl-user-role-update");
    assertThat(d.eventType()).isEqualTo("USER_ROLE_UPDATE");
    assertThat(d.totalDelta()).isEqualTo(2);
    assertThat(d.failDelta()).isEqualTo(1);
    assertThat(d.dupDelta()).isEqualTo(1);

    // 버킷 시작 시간 검증 (00:00:00Z)
    assertThat(d.bucketTime())
        .isEqualTo(Instant.parse("2025-12-26T00:00:00Z"));
  }
}
