package com.codeit.mopl.event.metrics;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventStats10mCollector {

  private static final long BUCKET_SECONDS = 600; // 10분

  private static final int MAX_KEYS = 50_000;

  private final KafkaEventStatsJdbcRepository repo;
  private final Clock clock;

  private static final class Acc {
    final LongAdder total = new LongAdder();
    final LongAdder fail  = new LongAdder();
    final LongAdder dup   = new LongAdder();

    void add(long t, long f, long d) {
      if (t != 0) total.add(t);
      if (f != 0) fail.add(f);
      if (d != 0) dup.add(d);
    }
  }

  private final Object lock = new Object();

  private Map<String, Acc> active = newEvictingMap();
  private Map<String, Acc> flushing = newEvictingMap();

  private final LongAdder droppedKeys = new LongAdder();

  private static Map<String, Acc> newEvictingMap() {
    return new LinkedHashMap<>(16, 0.75f, false) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Acc> eldest) {
        return size() > MAX_KEYS;
      }
    };
  }

  public void incTotal(KafkaEventKey key) { add(key, 1, 0, 0); }
  public void incFail(KafkaEventKey key)  { add(key, 0, 1, 0); }
  public void incDup(KafkaEventKey key)   { add(key, 0, 0, 1); }

  public void add(KafkaEventKey key, long total, long fail, long dup) {
    Instant bucket = currentBucket10m();
    String mapKey = key.topic() + "|" + key.eventType() + "|" + bucket.getEpochSecond();

    synchronized (lock) {
      int before = active.size();
      active.computeIfAbsent(mapKey, k -> new Acc()).add(total, fail, dup);

      if (active.size() < before) {
        droppedKeys.increment();
      }
    }
  }

  private Instant currentBucket10m() {
    long epoch = Instant.now(clock).getEpochSecond();
    return Instant.ofEpochSecond(epoch - (epoch % BUCKET_SECONDS));
  }

  @Scheduled(fixedDelay = 10_000)
  public void flushToDb() {
    Map<String, Acc> toFlush;

    synchronized (lock) {
      if (active.isEmpty()) return;
      Map<String, Acc> tmp = flushing;
      flushing = active;
      active = tmp;
      toFlush = flushing;
    }

    List<KafkaEventStatsDelta> deltas = new ArrayList<>(toFlush.size());

    for (var e : toFlush.entrySet()) {
      String mapKey = e.getKey();
      Acc acc = e.getValue();

      long t = acc.total.sumThenReset();
      long f = acc.fail.sumThenReset();
      long d = acc.dup.sumThenReset();
      if (t == 0 && f == 0 && d == 0) continue;

      String[] p = mapKey.split("\\|");
      deltas.add(new KafkaEventStatsDelta(
          p[0], p[1],
          Instant.ofEpochSecond(Long.parseLong(p[2])),
          t, f, d
      ));
    }

    if (deltas.isEmpty()) {
      toFlush.clear();
      return;
    }

    try {
      repo.upsertBatch10m(deltas);
      toFlush.clear();
    } catch (Exception ex) {
      log.error("[KAFKA_STATS] flush 실패. deltas={}, activeSize={}, droppedKeys={}",
          deltas.size(), sizeSafe(active), droppedKeys.sum(), ex);

      synchronized (lock) {
        int before = active.size();

        for (KafkaEventStatsDelta d : deltas) {
          String key = d.topic() + "|" + d.eventType() + "|" + d.bucketTime().getEpochSecond();
          active.computeIfAbsent(key, k -> new Acc())
              .add(d.totalDelta(), d.failDelta(), d.dupDelta());
        }

        // eviction 감지(대략)
        if (active.size() < before) {
          droppedKeys.increment();
        }

        toFlush.clear();
      }
    }
  }

  private static int sizeSafe(Map<?, ?> map) {
    return (map == null) ? 0 : map.size();
  }
}
