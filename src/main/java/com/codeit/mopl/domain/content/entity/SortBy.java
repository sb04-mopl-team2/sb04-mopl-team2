package com.codeit.mopl.domain.content.entity;

import com.codeit.mopl.exception.content.InvalidSortByException;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortBy {
  CREATED_AT("createdAt"),
  WATCHER_COUNT("watcherCount"),
  RATE("rate");

  private final String value;

  public static SortBy fromValue(String value) {
    for (SortBy sortBy : values()) {
      if (sortBy.value.equalsIgnoreCase(value)) {
        return sortBy;
      }
    }
    throw new InvalidSortByException(Map.of("inputValue", value));
  }
}