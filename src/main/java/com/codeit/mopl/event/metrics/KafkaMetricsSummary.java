package com.codeit.mopl.event.metrics;

import java.time.Instant;

public record KafkaMetricsSummary(
    // 식별
    String topic,
    String eventType,

    // 조회 범위 (UTC 기준)
    Instant fromUtc,
    Instant toUtc,

    // 버킷 (UTC 기준)
    Instant bucketStartUtc,
    Instant bucketEndUtc,

    // 집계 값
    long total,
    long fail,
    long dup,

    // 파생 지표
    double failureRate,
    double duplicateRate
) {}
