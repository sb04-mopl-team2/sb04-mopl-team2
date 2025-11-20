package com.codeit.mopl.domain.notification.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class MapperUtils {
  public static Instant asInstant(LocalDateTime time) {
    return time == null ? null : time.toInstant(ZoneOffset.UTC);
  }
}
