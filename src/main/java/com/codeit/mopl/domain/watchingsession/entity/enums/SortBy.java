package com.codeit.mopl.domain.watchingsession.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortBy {
  CREATED_AT("createdAt");
  private final String type;
}
