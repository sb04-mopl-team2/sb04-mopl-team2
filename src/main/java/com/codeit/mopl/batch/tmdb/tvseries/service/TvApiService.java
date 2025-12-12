package com.codeit.mopl.batch.tmdb.tvseries.service;

import com.codeit.mopl.batch.service.BatchMetricsService;
import com.codeit.mopl.batch.tmdb.base.service.AbstractTmdbApiService;
import com.codeit.mopl.batch.tmdb.tvseries.dto.TmdbDiscoverTvResponse;
import com.codeit.mopl.batch.tmdb.tvseries.dto.TmdbDiscoverTvResponse.TvShow;
import com.codeit.mopl.batch.tmdb.tvseries.mapper.TmdbTvMapper;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import java.net.URI;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Service
@Transactional
public class TvApiService extends AbstractTmdbApiService<
    TvShow,
    TmdbDiscoverTvResponse> {

  private final TmdbTvMapper tmdbTvMapper;

  public TvApiService(
      @Qualifier("tmdbWebClient") WebClient tmdbWebClient,
      ContentRepository contentRepository,
      BatchMetricsService metricsService,
      TmdbTvMapper tmdbTvMapper) {
    super(tmdbWebClient, contentRepository, metricsService);
    this.tmdbTvMapper = tmdbTvMapper;
  }

  @Override
  protected URI buildDiscoverUri(UriBuilder builder, LocalDate from, int page) {
    return builder
        .path("/discover/tv")
        .queryParam("language", "ko-KR")
        .queryParam("include_adult", "false")
        .queryParam("include_null_first_air_dates", "false")
        .queryParam("first_air_date.gte", from)
        .queryParam("first_air_date.lte", from)
        .queryParam("page", page)
        .queryParam("sort_by", "first_air_date.desc")
        .build();
  }

  @Override
  protected Content mapToContent(TmdbDiscoverTvResponse.TvShow tvShow) {
    return tmdbTvMapper.toContent(tvShow);
  }

  @Override
  protected Class<TmdbDiscoverTvResponse> getResponseClass() {
    return TmdbDiscoverTvResponse.class;
  }

  @Override
  protected String getContentType() {
    return "TV 프로그램";
  }

  @Override
  protected String getContentTypeForMetrics() {
    return "TV";
  }
}