package com.codeit.mopl.domain.content.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentType {
  MOVIE("movie"),
  TV("tvSeries"),
  SPORT("sport");

  private final String type;

  public static ContentType fromType(String type) {
    for (ContentType ct : values()) {
      if (ct.type.equals(type)) {
        return ct;
      }
    }
    throw new IllegalArgumentException("유효하지 않은 콘텐츠 타입입니다. 타입 : " + type);
  }
}
