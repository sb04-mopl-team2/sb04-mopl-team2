package com.codeit.mopl.domain.watchingsession;

import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final WatchingSessionSortByConverter watchingSessionSortByConverter;

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(watchingSessionSortByConverter);
  }
}