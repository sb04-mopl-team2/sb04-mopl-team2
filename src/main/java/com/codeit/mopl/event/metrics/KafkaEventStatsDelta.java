package com.codeit.mopl.event.metrics;

import java.time.Instant;

public record KafkaEventStatsDelta(
    String topic,
    String eventType,
    Instant bucketTime,
    long totalDelta,
    long failDelta,
    long dupDelta
) {}
