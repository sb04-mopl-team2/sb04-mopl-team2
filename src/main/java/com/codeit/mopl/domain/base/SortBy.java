package com.codeit.mopl.domain.base;

import lombok.Getter;

@Getter
public enum SortBy {
  CREATED_AT("createdAt"),
  UPDATED_AT("updatedAt"),
  SUBSCRIBER_COUNT("subscribeCount"),
  RATING("rating");

  private final String value;

  SortBy(String value) {
    this.value = value;
  }
}
