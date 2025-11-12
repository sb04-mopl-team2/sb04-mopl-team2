package com.codeit.mopl.domain.content.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentType {
  MOVIE("movie"),
  TV("tv"),
  SPORTS("sports");

  private final String type;
}
