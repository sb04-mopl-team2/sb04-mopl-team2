package com.codeit.mopl.domain.base;

import com.codeit.mopl.exception.global.InvalidInputValueException;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortDirection {
  ASCENDING("ASCENDING"),
  DESCENDING("DESCENDING");

  private final String value;

  public static SortDirection fromValue(String value) {
    if (value == null) {
      throw new InvalidInputValueException(Map.of("inputValue", "null"));
    }

    for (SortDirection direction : values()) {
      if (direction.value.equalsIgnoreCase(value)) {
        return direction;
      }
    }
    throw new InvalidInputValueException(Map.of("inputValue", value));
  }
}