package com.codeit.mopl.domain.base;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeUtil {

  public static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private TimeUtil() {}

  public static LocalDateTime toKst(Instant instant) {
    return instant == null ? null
        : instant.atZone(KST).toLocalDateTime();
  }

  public static Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.atZone(KST).toInstant();
  }
}
