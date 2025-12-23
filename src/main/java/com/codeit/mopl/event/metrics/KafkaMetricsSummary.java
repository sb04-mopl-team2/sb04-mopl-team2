package com.codeit.mopl.event.metrics;

import java.time.Instant;

public record KafkaMetricsSummary(
    // 식별
    String topic,
    String eventType,

    // 집계 범위(전체집계에서 사용)
    Instant fromUtc,
    Instant toUtc,
    Instant fromKst,
    Instant toKst,

    // 버킷(버킷집계에서 사용)
    Instant bucketStartUtc,
    Instant bucketEndUtc,
    Instant bucketStartKst,
    Instant bucketEndKst,

    // 집계 값
    long total,
    long fail,
    long dup,

    // 파생 지표
    double failureRate,
    double duplicateRate
) {}
