package com.codeit.mopl.domain.notification.entity;

public enum SortBy {
  CREATED_AT("createdAt");

  private final String field;

  SortBy(String field) {
    this.field = field;
  }

  public String field() {
    return field;
  }
}
