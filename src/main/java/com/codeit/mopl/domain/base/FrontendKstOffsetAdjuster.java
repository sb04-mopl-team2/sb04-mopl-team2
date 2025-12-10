package com.codeit.mopl.domain.base;

import java.time.Duration;
import java.time.Instant;
import org.mapstruct.Named;

public final class FrontendKstOffsetAdjuster {

  private static final Duration KST_OFFSET = Duration.ofHours(9);

  private FrontendKstOffsetAdjuster() {}

  @Named("adjustForFrontend")
  public static Instant adjust(Instant utcInstant) {
    return utcInstant == null ? null : utcInstant.plus(KST_OFFSET);
  }
}
