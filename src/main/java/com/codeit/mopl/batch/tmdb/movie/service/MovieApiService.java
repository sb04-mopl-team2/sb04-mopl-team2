package com.codeit.mopl.batch.tmdb.movie.service;

import com.codeit.mopl.batch.service.BatchMetricsService;
import com.codeit.mopl.batch.tmdb.base.service.AbstractTmdbApiService;
import com.codeit.mopl.batch.tmdb.movie.dto.TmdbDiscoverMovieResponse;
import com.codeit.mopl.batch.tmdb.movie.dto.TmdbDiscoverMovieResponse.Movie;
import com.codeit.mopl.batch.tmdb.movie.mapper.TmdbMovieMapper;
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
public class MovieApiService extends AbstractTmdbApiService<Movie, TmdbDiscoverMovieResponse> {

  private final TmdbMovieMapper tmdbMovieMapper;

  public MovieApiService(
      @Qualifier("tmdbWebClient") WebClient tmdbWebClient,
      ContentRepository contentRepository,
      BatchMetricsService metricsService,
      TmdbMovieMapper tmdbMovieMapper) {
    super(tmdbWebClient, contentRepository, metricsService);
    this.tmdbMovieMapper = tmdbMovieMapper;
  }

  @Override
  protected URI buildDiscoverUri(UriBuilder builder, LocalDate from, int page) {
    return builder
        .path("/discover/movie")
        .queryParam("region", "KR")
        .queryParam("language", "ko-KR")
        .queryParam("include_adult", "false")
        .queryParam("release_date.gte", from)
        .queryParam("release_date.lte", from)
        .queryParam("page", page)
        .queryParam("sort_by", "primary_release_date.desc")
        .build();
  }

  @Override
  protected Content mapToContent(TmdbDiscoverMovieResponse.Movie movie) {
    return tmdbMovieMapper.toContent(movie);
  }

  @Override
  protected Class<TmdbDiscoverMovieResponse> getResponseClass() {
    return TmdbDiscoverMovieResponse.class;
  }

  @Override
  protected String getContentType() {
    return "영화";
  }

  @Override
  protected String getContentTypeForMetrics() {
    return "MOVIE";
  }
}