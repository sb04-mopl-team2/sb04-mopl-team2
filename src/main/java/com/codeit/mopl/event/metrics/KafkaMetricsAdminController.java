package com.codeit.mopl.event.metrics;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KafkaMetricsAdminController {

  private final KafkaEventStatsQueryRepository queryRepository;

  //@PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/api/kafka-metrics/LastMinutes")
  public List<KafkaMetricsSummary> kafkaMetricsLastMinutes(@RequestParam(defaultValue = "60") int minutes) {
    return queryRepository.aggregateLastMinutes(minutes);
  }

  @GetMapping("/api/kafka-metrics/BucketLastMinutes")
  public List<KafkaMetricsSummary> kafkaMetricsBucketLastMinutes(@RequestParam(defaultValue = "60") int minutes) {
    return queryRepository.aggregateByBucketLastMinutes(minutes);
  }

}
