package com.codeit.mopl.batch.tmdb.service;

import com.codeit.mopl.batch.tmdb.dto.TmdbDiscoverMovieResponse;
import com.codeit.mopl.batch.tmdb.mapper.TmdbMovieMapper;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.search.ContentESRepository;
import com.codeit.mopl.search.converter.ContentConverter;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbApiService {

  @Qualifier("tmdbWebClient")
  private final WebClient tmdbWebClient;

  private final TmdbMovieMapper tmdbMovieMapper;
  private final ContentRepository contentRepository;

  private final ContentESRepository contentESRepository;
  private final ContentConverter converter;
  private final ContentMapper contentMapper;


  @Transactional
  public Mono<List<Content>> discoverMoviesFromDate(LocalDate from, int page) {
    log.info("[TMDB] 영화 조회 시작 from = {}, page = {}", from, page);

    Mono<TmdbDiscoverMovieResponse> tmdbDiscoverMovieResponseMono = tmdbWebClient
        .get()
        .uri(uriBuilder -> buildDiscoverMoviesFromDateUri(uriBuilder, from, page))
        .retrieve()
        .bodyToMono(TmdbDiscoverMovieResponse.class);

    return tmdbDiscoverMovieResponseMono
        .flatMapMany(response ->
            Flux.fromIterable(response.getResults() != null ? response.getResults() : List.of())
        )
        .map(tmdbMovieMapper::toContent)
        .publishOn(Schedulers.boundedElastic())
//        .map(contentRepository::save)
        .map(c -> {
          Content savedContent = contentRepository.save(c);
          contentESRepository.save(converter.convertToDocument(contentMapper.toDto(savedContent), savedContent.getCreatedAt()));
          return savedContent;
        })
        .collectList()
        .doOnSuccess(list -> log.info("[TMDB] 영화 조회 완료 저장된 컨텐츠 수 = {}", list.size()));
  }

  /**
   * 특정 날짜 영화 조회 및 저장 (응답 객체 반환) 첫 페이지 호출 시 totalPages 정보를 함께 반환받기 위해 사용
   *
   * @param from 개봉일
   * @param page 페이지 번호
   * @return Mono<TmdbDiscoverMovieResponse> API 응답 전체
   */
  @Transactional
  public Mono<TmdbDiscoverMovieResponse> discoverMoviesFromDateWithResponse(LocalDate from, int page) {
    log.info("[TMDB] 영화 조회(응답 포함) 시작 from = {}, page = {}", from, page);

    return tmdbWebClient
        .get()
        .uri(uriBuilder -> buildDiscoverMoviesFromDateUri(uriBuilder, from, page))
        .retrieve()
        .bodyToMono(TmdbDiscoverMovieResponse.class)
        .publishOn(Schedulers.boundedElastic())
        .doOnNext(response -> {
          Flux.fromIterable(response.getResults())
              .map(tmdbMovieMapper::toContent)
              .publishOn(Schedulers.boundedElastic())
//              .doOnNext(contentRepository::save)
              .doOnNext(c -> {
                Content savedContent = contentRepository.save(c);
                contentESRepository.save(converter.convertToDocument(contentMapper.toDto(savedContent), savedContent.getCreatedAt()));
              })
              .blockLast();
          log.info("[TMDB] 영화 조회 및 저장 완료 조회 수 = {}", response.getResults().size());
        });
  }

  private URI buildDiscoverMoviesFromDateUri(UriBuilder builder, LocalDate from, int page) {
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