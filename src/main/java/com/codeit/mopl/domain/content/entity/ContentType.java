package com.codeit.mopl.domain.content.entity;

import com.codeit.mopl.exception.content.InvalidContentTypeException;
import java.util.Map;
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
    throw new InvalidContentTypeException(Map.of("inputValue", type));
  }
}
