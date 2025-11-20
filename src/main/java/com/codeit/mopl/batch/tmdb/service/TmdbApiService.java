package com.codeit.mopl.batch.tmdb.service;

import com.codeit.mopl.batch.tmdb.dto.TmdbDiscoverMovieResponse;
import com.codeit.mopl.batch.tmdb.mapper.TmdbMovieMapper;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class TmdbApiService {

  @Qualifier("tmdbWebClient")
  private final WebClient tmdbWebClient;

  private final TmdbMovieMapper tmdbMovieMapper;
  private final ContentRepository contentRepository;

  /**
   * 특정 날짜 영화 조회 및 저장 GET /discover/movie?release_date.gte=...&release_date.lte=...
   *
   * @param from 개봉일
   * @param page 페이지 번호
   * @return Mono<List < Content>> 저장된 컨텐츠 리스트
   */
  @Transactional
  public Mono<List<Content>> discoverMoviesFromDate(
      LocalDate from,
      int page
  ) {
    Mono<TmdbDiscoverMovieResponse> tmdbDiscoverMovieResponseMono = tmdbWebClient
        .get()
        .uri(uriBuilder -> buildDiscoverMoviesFromDateUri(uriBuilder, from, page))
        .retrieve()
        .bodyToMono(TmdbDiscoverMovieResponse.class);

    return tmdbDiscoverMovieResponseMono
        .flatMapMany(response -> Flux.fromIterable(response.getResults()))
        .map(tmdbMovieMapper::toContent)
        .publishOn(Schedulers.boundedElastic())
        .doOnNext(contentRepository::save)
        .collectList();
  }

  /**
   * 특정 날짜 영화 조회 및 저장 (응답 객체 반환) 첫 페이지 호출 시 totalPages 정보를 함께 반환받기 위해 사용
   *
   * @param from 개봉일
   * @param page 페이지 번호
   * @return Mono<TmdbDiscoverMovieResponse> API 응답 전체
   */
  @Transactional
  public Mono<TmdbDiscoverMovieResponse> discoverMoviesFromDateWithResponse(
      LocalDate from,
      int page
  ) {
    return tmdbWebClient
        .get()
        .uri(uriBuilder -> buildDiscoverMoviesFromDateUri(uriBuilder, from, page))
        .retrieve()
        .bodyToMono(TmdbDiscoverMovieResponse.class)
        .publishOn(Schedulers.boundedElastic())  // Netty 스레드에서 즉시 전환!
        .doOnNext(response -> {
          // 이제 boundedElastic 스레드에서 안전하게 실행
          Flux.fromIterable(response.getResults())
              .map(tmdbMovieMapper::toContent)
              .publishOn(Schedulers.boundedElastic())
              .doOnNext(contentRepository::save)
              .blockLast();
        });
  }

  private URI buildDiscoverMoviesFromDateUri(
      UriBuilder builder,
      LocalDate from,
      int page
  ) {
    return builder
        .path("/discover/movie")
        .queryParam("region", "KR")
        .queryParam("language", "ko-KR")
        .queryParam("release_date.gte", from)
        .queryParam("release_date.lte", from)
        .queryParam("page", page)
        .queryParam("sort_by", "primary_release_date.desc")
        .build();
  }
}