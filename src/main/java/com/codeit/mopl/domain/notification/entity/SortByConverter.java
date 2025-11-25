package com.codeit.mopl.domain.notification.entity;

import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SortByConverter implements Converter<String, SortBy> {

  @Override
  public SortBy convert(String source) {
    if (source == null) {
      return null;
    }

    return Arrays.stream(SortBy.values())
        .filter(sortBy -> sortBy.getType().equalsIgnoreCase(source))  // "createdAt" 매칭
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid sortBy: " + source));
  }
}