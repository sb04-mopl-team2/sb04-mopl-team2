package com.codeit.mopl.domain.watchingsession;

import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class WatchingSessionSortByConverter implements Converter<String, SortBy> {

  @Override
  public SortBy convert(String source) {
    return Arrays.stream(SortBy.values())
        .filter(sortBy -> sortBy.getType().equalsIgnoreCase(source))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid sortBy: " + source));
  }


}
