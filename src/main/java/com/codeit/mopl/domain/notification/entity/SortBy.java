package com.codeit.mopl.domain.notification.entity;

import lombok.Getter;

@Getter
public enum SortBy {
  CREATED_AT("createdAt");

  private final String type;

  SortBy(String type) {
    this.type = type;
  }
}
